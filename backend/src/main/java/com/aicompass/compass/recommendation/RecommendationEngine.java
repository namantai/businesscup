package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.recommendation.CostCalculator.*;
import static com.aicompass.compass.catalog.CatalogTypes.CapabilityState;

/** Pure deterministic use case: fixed profile, immutable catalog, policy and time. No IO/LLM. */
public final class RecommendationEngine {
    public static final String ENGINE_VERSION = "1.0.1";
    public static final String POLICY_VERSION = "p04-1";
    private final com.aicompass.compass.shared.money.Money fee;
    public RecommendationEngine() { this(com.aicompass.compass.shared.money.Money.of("0", "USD")); }
    // Server policy, never a price/entitlement accepted from an untrusted preview request.
    RecommendationEngine(com.aicompass.compass.shared.money.Money fee) {
        this.fee = fee;
    }
    public RecommendationResult calculate(RecommendationProfile profile, PublishedCatalogSnapshot catalog, Instant asOf) {
        var warnings = new ArrayList<Reason>();
        if (catalog == null) return new RecommendationResult(null, null, "EMPTY", ENGINE_VERSION, policyVersion(), asOf, true,
                new Search(0,0,0,0,0,0,"No published catalog"), "NO_PUBLISHED_CATALOG", List.of(), List.of(), List.of(),
                List.of(reason(Code.NO_PUBLISHED_CATALOG, "No snapshot has been activated")), List.of(), List.of(), List.of(), List.of());
        if (asOf.isBefore(catalog.publishedAsOf())) throw ProfileValidator.bad("asOf precedes the active catalog snapshot");
        var evidence = new EvidenceAssessment(catalog, asOf);
        var coverage = new CapabilityCoverage(catalog, profile, evidence);
        var costs = new CostCalculator(catalog, evidence, profile, asOf);
        var resolved = CandidateResolver.resolve(catalog, profile, evidence, coverage, costs, asOf);
        var feeFx = new ArrayList<FxUsed>();
        BigDecimal convertedFee = costs.convert(fee.amount(), fee.currency().getCurrencyCode(), feeFx, warnings);
        var search = new BundleSearch(profile, coverage, new UsageAssessment(catalog, evidence, coverage, asOf), convertedFee).search(resolved);
        boolean complete = resolved.complete() && search.usageSearchComplete();
        var grouped = new LinkedHashMap<String,List<Strategy>>(); var selections = new LinkedHashMap<String,BundleSearch.Selection>();
        List<Strategy> order = switch (profile.priority()) {
            case ECONOMY -> List.of(Strategy.ECONOMY, Strategy.BALANCED, Strategy.EXPANDED);
            case BALANCE -> List.of(Strategy.BALANCED, Strategy.ECONOMY, Strategy.EXPANDED);
            case CAPABILITIES -> List.of(Strategy.EXPANDED, Strategy.BALANCED, Strategy.ECONOMY);
        };
        for (Strategy strategy : order) {
            var selection = search.selected().get(strategy);
            if (selection != null) { grouped.computeIfAbsent(selection.key(), k -> new ArrayList<>()).add(strategy); selections.put(selection.key(), selection); }
        }
        BigDecimal baseline = resolved.existing().isEmpty() ? null : BigDecimal.ZERO;
        for (var c : resolved.existing()) baseline = add(baseline, c.costs.recurring());
        final BigDecimal comparisonBaseline = baseline;
        var bundles = selections.values().stream().map(s -> bundle(s, grouped.get(s.key()), profile, catalog, coverage, comparisonBaseline, convertedFee, asOf)).toList();
        var alternatives = search.conditional().stream().map(s -> bundle(s, List.of(), profile, catalog, coverage, comparisonBaseline, convertedFee, asOf)).toList();
        warnings.add(reason(Code.LIMITED_CATALOG, "Only " + catalog.products().size() + " catalog products; this is not the whole AI market"));
        warnings.add(reason(Code.UNKNOWN_QUALITY, "Functional coverage is not an independent model-quality ranking; no comparable quality winner is claimed"));
        if (!complete) warnings.add(reason(Code.SEARCH_TRUNCATED, "Candidate cap 40 or quota-allocation cap 10000 reached; results are not globally optimal"));
        if (bundles.size() < 3 && !bundles.isEmpty()) warnings.add(reason(Code.NO_DISTINCT_ALTERNATIVE, "Strategies share a result; duplicate bundles are not returned"));
        if (search.retainedConflict()) warnings.add(reason(Code.RETAINED_CONFLICT, "Must-keep subscriptions conflict, exceed maxTools or fail a hard availability constraint"));
        if (bundles.isEmpty()) {
            if (search.budgetRejected() > 0) warnings.add(reason(Code.BUDGET_CONFLICT, "Considered combinations exceed the recurring budget"));
            if (search.upfrontRejected() > 0) warnings.add(reason(Code.UPFRONT_CONFLICT, "Considered combinations exceed the cash-due-now limit"));
            if (search.usageRejected() > 0) warnings.add(reason(Code.USAGE_UNKNOWN, "Required usage could not be confirmed within shared quotas"));
            warnings.add(reason(Code.NO_MATCH, "No confirmed admissible bundle; review unmet requirements and candidate reasons"));
        }
        var relevant = new ArrayList<>(resolved.mandatory()); relevant.addAll(resolved.optional());
        var unmet = coverage.combine(relevant).stream().filter(c -> c.required() && !c.confirmed() && !c.acceptedForRequirement())
                .map(c -> new Unmet(c.capabilityId(), c.state(), c.state() == CapabilityState.UNKNOWN ? Code.UNKNOWN_CAPABILITY : Code.MISSING_REQUIRED_FEATURE)).toList();
        var fx = java.util.stream.Stream.concat(relevant.stream().flatMap(c -> c.costs.fx().stream()), feeFx.stream()).distinct().sorted(Comparator.comparing(FxUsed::id)).toList();
        return new RecommendationResult(null, catalog.version(), catalog.datasetKind(), ENGINE_VERSION, policyVersion(), asOf, complete,
                new Search(catalog.products().size(), resolved.total(), relevant.size(), search.visited(), search.feasible(), search.dominated(),
                        complete ? "Complete over admissible candidates in this limited catalog" : "Candidate or quota-allocation bound; deterministic subset"),
                bundles.isEmpty() ? (alternatives.isEmpty() ? "NO_MATCH" : "CONDITIONAL_ONLY") : "MATCH", bundles, alternatives,
                unmet, List.copyOf(warnings), resolved.assessments(), evidence.publicEvidence(), catalog.sources(), fx);
    }
    private String policyVersion() { return POLICY_VERSION + "-fee-" + fee.currency().getCurrencyCode() + "-" + decimal(fee.amount()); }
    private Bundle bundle(BundleSearch.Selection s, List<Strategy> strategies, RecommendationProfile p, PublishedCatalogSnapshot catalog,
                          CapabilityCoverage coverage, BigDecimal baseline, BigDecimal convertedFee, Instant asOf) {
        var factual = new ArrayList<Reason>(); var policy = new ArrayList<Reason>(); var conditions = new TreeSet<String>();
        var items = new ArrayList<Item>(); boolean userCosts = false;
        for (var c : s.items()) {
            factual.addAll(c.reasons); userCosts |= c.existing != null;
            var ids = new TreeSet<String>(); c.states.forEach(state -> ids.addAll(state.evidenceIds())); c.reasons.forEach(r -> ids.addAll(r.evidenceIds()));
            if (c.offer != null) {
                ids.addAll(List.of(c.offer.priceEvidenceId(), c.offer.taxEvidenceId(), c.offer.feeEvidenceId(), c.offer.regionEvidenceId()));
                conditions.add(c.offer.conditions());
            }
            for (var e : catalog.evidence()) if (ids.contains(e.id())) conditions.add(e.conditions());
            String currency = c.existing == null ? c.offer.currency() : c.existing.currency();
            String charge = c.existing == null ? c.offer.amount() : c.existing.amount();
            String period = c.existing == null ? c.offer.billingPeriod().name() : c.existing.billingPeriod().name();
            items.add(new Item(c.id, c.plan == null ? null : c.plan.productId(), c.plan == null ? null : c.plan.id(),
                    c.offer == null ? null : c.offer.id(), c.existing == null ? Action.ADD : Action.KEEP, currency, charge, period,
                    c.existing != null ? c.existing.nextRenewal() : period.equals("MONTH") || period.equals("YEAR")
                            ? asOf.atZone(java.time.ZoneOffset.UTC).toLocalDate().plusMonths(period.equals("YEAR") ? 12 : 1) : null,
                    c.agents, c.models, c.pools, List.copyOf(ids)));
        }
        factual.addAll(s.usage().reasons());
        if (s.confirmed()) policy.add(reason(Code.WITHIN_BUDGET, "Known costs satisfy recurring and immediate cash limits; not a purchase guarantee"));
        if (fee.amount().signum() > 0) policy.add(reason(Code.COMPASS_FEE, "Server-selected recurring Compass fee is included in all totals"));
        policy.add(reason(Code.UNKNOWN_QUALITY, "W uses only confirmed function weights; PARTIAL has no arbitrary 50% quality score"));
        if (strategies.contains(Strategy.BALANCED)) policy.add(reason(Code.WITHIN_BUDGET, "BALANCED tolerance is 0.05 of weighted coverage, a versioned product policy"));
        var actions = p.existingSubscriptions().stream().map(existing -> {
            boolean keep = s.items().stream().anyMatch(c -> c.existing != null && c.existing.id().equals(existing.id()));
            return new PortfolioAction(existing.id(), keep ? Action.KEEP : Action.CANCEL_AT_RENEWAL,
                    keep ? "Retained cost and verified functions counted once" : "Proposed cancellation at " + existing.nextRenewal() + "; no refund or automatic action; already-paid cost is not recoverable");
        }).toList();
        var costResult = new Costs(p.budget().currency(), decimal(s.now()), decimal(s.upfront()), decimal(s.monthly()), decimal(s.horizon()),
                decimal(s.monthly()), decimal(baseline), baseline == null || s.monthly() == null ? null : decimal(baseline.subtract(s.monthly())), decimal(convertedFee),
                !s.confirmed() ? Confidence.INCOMPLETE : userCosts ? Confidence.ESTIMATED : Confidence.CONFIRMED_COMPONENTS, s.confirmed());
        return new Bundle(s.key(), List.copyOf(strategies), s.confirmed() ? Eligibility.ELIGIBLE : s.items().stream().map(c -> c.eligibility).reduce(convertedFee == null ? Eligibility.UNKNOWN : Eligibility.CONDITIONAL, EligibilityAssessment::merge),
                List.copyOf(items), costResult, coverage.combine(s.items()), s.usage().fit(), factual.stream().distinct().toList(), List.copyOf(policy), List.copyOf(conditions), actions);
    }
}
