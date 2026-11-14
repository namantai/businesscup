package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.recommendation.CostCalculator.reason;

final class UsageAssessment {
    record Result(boolean hardFit, String fit, boolean searchComplete, List<Reason> reasons) {}
    private final PublishedCatalogSnapshot catalog;
    private final EvidenceAssessment evidence;
    private final Instant asOf;
    private final CapabilityCoverage coverage;
    UsageAssessment(PublishedCatalogSnapshot catalog, EvidenceAssessment evidence, CapabilityCoverage coverage, Instant asOf) {
        this.coverage = coverage; this.catalog = catalog; this.evidence = evidence; this.asOf = asOf;
    }
    Result assess(List<Candidate> candidates, RecommendationProfile profile) {
        if (profile.requirements().usage().isEmpty()) return new Result(true, "UNKNOWN", true,
                List.of(reason(Code.USAGE_UNKNOWN, "Frequency is not a measured volume; sufficient dynamic limits are not guaranteed")));
        var demands = profile.requirements().usage();
        var required = demands.stream().filter(RecommendationProfile.Usage::required).toList();
        var pools = new HashMap<String,CatalogManifest.QuotaPool>(); catalog.pools().forEach(p -> pools.put(p.id(), p));
        Set<String> offers = new HashSet<>(); candidates.forEach(c -> { if (c.offer != null) offers.add(c.offer.id()); });
        var choices = new ArrayList<List<Consumption>>(); boolean unknown = false;
        for (var d : required) {
            var options = new ArrayList<Consumption>();
            for (var l : catalog.limits()) {
                if (!offers.contains(l.offerId()) || !l.capabilityId().equals(d.capabilityId()) || !l.mode().equals(d.mode())) continue;
                boolean supported = candidates.stream().filter(c -> c.offer != null && c.offer.id().equals(l.offerId())).anyMatch(c -> {
                    var state = coverage.state(c.plan.productId(), c.plan.id(), d.capabilityId());
                    return state.confirmed() || state.accepted();
                });
                if (!supported) { unknown = true; continue; }
                var pool = pools.get(l.poolId());
                if (!d.unit().equals("actions") || l.unitsPerUse() == null || !evidence.confirmed(pool.evidenceId())
                        || (pool.expiresAt() != null && !pool.expiresAt().isAfter(asOf.atZone(java.time.ZoneOffset.UTC).plusMonths(profile.horizonMonths()).toInstant()))
                        || pool.state() == LimitState.UNKNOWN || (pool.resetPeriod() != ResetPeriod.MONTH && pool.state() != LimitState.UNLIMITED)) { unknown = true; continue; }
                BigDecimal amount = new BigDecimal(d.monthlyQuantity()).multiply(new BigDecimal(l.unitsPerUse()));
                options.add(new Consumption(pool.id(), amount, pool.state() == LimitState.UNLIMITED ? null : new BigDecimal(pool.quantity())));
            }
            if (options.isEmpty()) unknown = true;
            choices.add(options);
        }
        int[] attempts = {0}; boolean fit = allocate(choices, 0, new HashMap<>(), attempts);
        if (fit) return new Result(true, demands.size() == required.size() ? "CONFIRMED" : "UNKNOWN", true,
                List.of(reason(Code.SHARED_QUOTA, "Required monthly mode-qualified actions fit without counting any shared pool twice")));
        return new Result(false, "UNKNOWN", attempts[0] <= 10000, List.of(reason(unknown || attempts[0] > 10000 ? Code.USAGE_UNKNOWN : Code.USAGE_EXCEEDED,
                "Required usage has no verified allocation; no cross-provider credit conversion or assumed daily-to-monthly allowance")));
    }
    private record Consumption(String pool, BigDecimal amount, BigDecimal capacity) {}
    private boolean allocate(List<List<Consumption>> choices, int index, Map<String,BigDecimal> used, int[] attempts) {
        if (++attempts[0] > 10000) return false;
        if (index == choices.size()) return true;
        for (var c : choices.get(index)) {
            BigDecimal before = used.getOrDefault(c.pool(), BigDecimal.ZERO), after = before.add(c.amount());
            if (c.capacity() != null && after.compareTo(c.capacity()) > 0) continue;
            used.put(c.pool(), after);
            if (allocate(choices, index + 1, used, attempts)) return true;
            used.put(c.pool(), before);
        }
        return false;
    }
}
