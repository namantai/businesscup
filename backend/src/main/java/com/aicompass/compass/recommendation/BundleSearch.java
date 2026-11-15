package com.aicompass.compass.recommendation;

import java.math.BigDecimal;
import java.util.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;

/** Exhaustive bounded combinations with monotonic cost/conflict pruning, no brand/affiliate objective. */
final class BundleSearch {
    record Selection(List<Candidate> items, int score, BigDecimal monthly, BigDecimal now, BigDecimal upfront,
                     BigDecimal horizon, boolean confirmed, String key, UsageAssessment.Result usage) {}
    record Outcome(Map<Strategy,Selection> selected, List<Selection> conditional, long visited, long feasible,
                   long dominated, long budgetRejected, long upfrontRejected, long featureRejected, long usageRejected,
                   boolean retainedConflict, boolean usageSearchComplete) {}
    private final RecommendationProfile profile;
    private final CapabilityCoverage coverage;
    private final UsageAssessment usage;
    private final BigDecimal fee;
    private final NavigableMap<Integer,Selection> bestByScore = new TreeMap<>();
    private final List<Selection> conditional = new ArrayList<>();
    private boolean usageSearchComplete = true;
    private long visited, feasible, dominated, budgetRejected, upfrontRejected, featureRejected, usageRejected;
    BundleSearch(RecommendationProfile profile, CapabilityCoverage coverage, UsageAssessment usage, BigDecimal fee) {
        this.profile = profile; this.coverage = coverage; this.usage = usage; this.fee = fee;
    }
    Outcome search(CandidateResolver.Resolution resolved) {
        var mandatory = resolved.mandatory();
        boolean conflict = mandatory.size() > profile.maxTools() || mandatory.stream().anyMatch(c -> c.eligibility == Eligibility.INELIGIBLE);
        for (int i = 0; i < mandatory.size(); i++) for (int j = 0; j < i; j++) if (incompatible(mandatory.get(i), mandatory.get(j))) conflict = true;
        if (!conflict) enumerate(new ArrayList<>(mandatory), resolved.optional(), 0);
        var winners = new EnumMap<Strategy,Selection>(Strategy.class);
        if (!bestByScore.isEmpty()) {
            winners.put(Strategy.ECONOMY, bestByScore.values().stream().min(BundleSearch::economy).orElseThrow());
            int best = bestByScore.lastKey();
            // Integer comparison implements the policy tolerance 0.05 without floating-point weights.
            winners.put(Strategy.BALANCED, bestByScore.entrySet().stream().filter(e -> (long)(best - e.getKey()) * 100 <= (long)coverage.totalWeight * 5)
                    .map(Map.Entry::getValue).min(BundleSearch::cheaperSimpler).orElseThrow());
            winners.put(Strategy.EXPANDED, bestByScore.lastEntry().getValue());
        }
        return new Outcome(winners, List.copyOf(conditional), visited, feasible, dominated, budgetRejected, upfrontRejected, featureRejected, usageRejected, conflict, usageSearchComplete);
    }
    private void enumerate(ArrayList<Candidate> items, List<Candidate> optional, int from) {
        // Counts search nodes, including the initial empty (or mandatory-only) root.
        visited++;
        BigDecimal monthly = fee, now = fee, upfront = fee, horizon = fee == null ? null : fee.multiply(BigDecimal.valueOf(profile.horizonMonths()));
        BigDecimal knownMonthly = fee == null ? BigDecimal.ZERO : fee, knownNow = knownMonthly;
        boolean confirmed = fee != null;
        var mask = new BitSet(); var accepted = new BitSet();
        for (var c : items) {
            monthly = CostCalculator.add(monthly, c.costs.recurring()); now = CostCalculator.add(now, c.costs.now());
            upfront = CostCalculator.add(upfront, c.costs.upfront()); horizon = CostCalculator.add(horizon, c.costs.horizon());
            if (c.costs.recurringLowerBound() != null) knownMonthly = knownMonthly.add(c.costs.recurringLowerBound());
            if (c.costs.nowLowerBound() != null) knownNow = knownNow.add(c.costs.nowLowerBound());
            confirmed &= c.eligibility == Eligibility.ELIGIBLE && c.costs.confirmed();
            mask.or(c.confirmed); accepted.or(c.accepted);
        }
        if (knownMonthly.compareTo(new BigDecimal(profile.budget().monthlyMax())) > 0) { budgetRejected++; return; }
        if (knownNow.compareTo(new BigDecimal(profile.budget().upfrontMax())) > 0) { upfrontRejected++; return; }
        var hard = (BitSet) mask.clone(); hard.or(accepted);
        var missing = (BitSet) coverage.required.clone(); missing.andNot(hard);
        if (items.isEmpty() || !missing.isEmpty()) featureRejected++;
        else {
            var used = usage.assess(items, profile);
            usageSearchComplete &= used.searchComplete();
            if (!used.hardFit()) usageRejected++;
            else {
                String key = items.stream().map(c -> c.id).sorted().reduce((a,b) -> a + "+" + b).orElse("");
                var selection = new Selection(List.copyOf(items), coverage.score(mask), monthly, now, upfront, horizon, confirmed, key, used);
                if (confirmed) {
                    feasible++;
                    if (redundant(items, mask, accepted)) dominated++;
                    else bestByScore.merge(selection.score(), selection, (a,b) -> cheaperSimpler(a,b) <= 0 ? a : b);
                } else {
                    conditional.add(selection); conditional.sort(BundleSearch::economy);
                    if (conditional.size() > 3) conditional.removeLast();
                }
            }
        }
        if (items.size() == profile.maxTools()) return;
        for (int i = from; i < optional.size(); i++) {
            Candidate candidate = optional.get(i);
            if (items.stream().anyMatch(c -> incompatible(c, candidate))) continue;
            items.add(candidate); enumerate(items, optional, i + 1); items.removeLast();
        }
    }
    private boolean redundant(List<Candidate> items, BitSet confirmed, BitSet accepted) {
        if (!profile.requirements().usage().isEmpty()) return false;
        for (var candidate : items) {
            if (candidate.mandatory || candidate.existing != null) continue;
            var without = new BitSet(); var acceptedWithout = new BitSet();
            for (var other : items) if (other != candidate) { without.or(other.confirmed); acceptedWithout.or(other.accepted); }
            if (without.equals(confirmed) && acceptedWithout.equals(accepted) && items.size() > 1) return true;
        }
        return false;
    }
    static boolean incompatible(Candidate a, Candidate b) {
        return a.productId().equals(b.productId()) || (a.conflictGroup() != null && a.conflictGroup().equals(b.conflictGroup()));
    }
    private static int compare(BigDecimal a, BigDecimal b) { return a == null ? (b == null ? 0 : 1) : b == null ? -1 : a.compareTo(b); }
    static int cheaperSimpler(Selection a, Selection b) {
        int result = compare(a.monthly(), b.monthly());
        if (result == 0) result = Integer.compare(a.items().size(), b.items().size());
        if (result == 0) result = Long.compare(a.items().stream().filter(c -> c.existing == null).count(), b.items().stream().filter(c -> c.existing == null).count());
        if (result == 0) result = compare(a.now(), b.now());
        if (result == 0) result = a.key().compareTo(b.key());
        return result;
    }
    static int economy(Selection a, Selection b) {
        int result = compare(a.monthly(), b.monthly());
        if (result == 0) result = Integer.compare(b.score(), a.score());
        return result == 0 ? cheaperSimpler(a,b) : result;
    }
}
