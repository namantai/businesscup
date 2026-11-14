package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.time.Instant;
import java.util.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.recommendation.CostCalculator.reason;
import static com.aicompass.compass.catalog.CatalogTypes.*;

final class CandidateResolver {
    record Resolution(List<Candidate> mandatory, List<Candidate> optional, List<CandidateAssessment> assessments,
                      List<Candidate> existing, int total, boolean complete) {}
    static Resolution resolve(PublishedCatalogSnapshot catalog, RecommendationProfile p, EvidenceAssessment evidence,
                              CapabilityCoverage coverage, CostCalculator calculator, Instant asOf) {
        return resolve(catalog,p,evidence,coverage,calculator,asOf,Map.of());
    }
    static Resolution resolve(PublishedCatalogSnapshot catalog, RecommendationProfile p, EvidenceAssessment evidence,
            CapabilityCoverage coverage, CostCalculator calculator, Instant asOf, Map<String,CostCalculator.Amounts> ledgerCosts) {
        var eligibility = new EligibilityAssessment(catalog, p, evidence, coverage, asOf);
        var candidates = new ArrayList<Candidate>(); var assessments = new ArrayList<CandidateAssessment>();
        var retained = new ArrayList<Candidate>();
        var plans = new HashMap<String,CatalogManifest.SubscriptionPlan>(); catalog.plans().forEach(plan -> plans.put(plan.id(), plan));
        var offers = new HashMap<String,CatalogManifest.PriceOffer>(); catalog.offers().forEach(o -> offers.put(o.id(), o));
        for (String id : p.acceptedConditionEvidenceIds()) if (evidence.get(id) == null) throw ProfileValidator.bad("Accepted evidence id is not in the active snapshot");
        for (var s : p.existingSubscriptions()) {
            var plan = plans.get(s.planId()); var offer = offers.get(s.offerId());
            if (s.planId() != null && (plan == null || offer == null || !offer.planId().equals(plan.id())))
                throw ProfileValidator.bad("Existing plan/offer is not in the active snapshot; use a custom subscription with null references");
            var assessed = plan == null ? new EligibilityAssessment.Assessment(Eligibility.UNKNOWN,
                    List.of(reason(Code.UNKNOWN_CAPABILITY, "Custom subscription counts toward cost; its functions and constraints are unknown"))) : eligibility.assess(plan, offer, true);
            var costs = ledgerCosts.containsKey(s.id()) ? ledgerCosts.get(s.id()) : calculator.existing(s);
            var reasons = new ArrayList<>(assessed.reasons()); reasons.addAll(costs.reasons()); reasons.add(reason(Code.KEEP_EXISTING, "Existing subscription is counted once"));
            var status = costs.confirmed() ? assessed.status() : EligibilityAssessment.merge(assessed.status(), Eligibility.UNKNOWN);
            var candidate = build("existing:" + s.id(), plan, offer, s,
                    s.mustKeep() || p.portfolioMode() == RecommendationProfile.PortfolioMode.AUGMENT,
                    status, costs, reasons, catalog, coverage, evidence);
            retained.add(candidate); candidates.add(candidate);
        }
        Set<String> existingPlans = new HashSet<>(); p.existingSubscriptions().forEach(s -> { if (s.planId() != null) existingPlans.add(s.planId()); });
        for (var offer : catalog.offers().stream().sorted(Comparator.comparing(CatalogManifest.PriceOffer::id)).toList()) {
            if (existingPlans.contains(offer.planId())) continue; // Keeping is a candidate; never buy the same entitlement twice.
            var plan = plans.get(offer.planId()); var assessed = eligibility.assess(plan, offer, false);
            var costs = calculator.offer(offer); var reasons = new ArrayList<>(assessed.reasons()); reasons.addAll(costs.reasons());
            var status = costs.confirmed() ? assessed.status() : EligibilityAssessment.merge(assessed.status(), Eligibility.CONDITIONAL);
            if (offer.billingPeriod() == BillingPeriod.YEAR) reasons.add(reason(Code.ANNUAL_UPFRONT_REQUIRED, "Annual monthly equivalent requires a full annual payment", offer.priceEvidenceId()));
            var c = build("offer:" + offer.id(), plan, offer, null, false, status, costs, reasons, catalog, coverage, evidence);
            candidates.add(c);
        }
        candidates.sort(Comparator.comparing(c -> c.id));
        for (var c : candidates) assessments.add(new CandidateAssessment(c.id, c.eligibility, c.reasons));
        var mandatory = candidates.stream().filter(c -> c.mandatory).toList();
        var optional = candidates.stream().filter(c -> !c.mandatory && c.eligibility != Eligibility.INELIGIBLE).toList();
        int total = mandatory.size() + optional.size();
        // Never silently truncate. Mandatory entries are never discarded to make room.
        int available = Math.max(0, 40 - mandatory.size());
        boolean complete = optional.size() <= available;
        optional = optional.subList(0, Math.min(available, optional.size()));
        return new Resolution(mandatory, optional, List.copyOf(assessments), List.copyOf(retained), total, complete);
    }
    private static Candidate build(String id, CatalogManifest.SubscriptionPlan plan, CatalogManifest.PriceOffer offer,
            RecommendationProfile.ExistingSubscription existing, boolean mandatory, Eligibility status, CostCalculator.Amounts costs,
            List<Reason> reasons, PublishedCatalogSnapshot catalog, CapabilityCoverage coverage, EvidenceAssessment evidence) {
        var states = coverage.targets.stream().map(t -> coverage.state(plan == null ? null : plan.productId(), plan == null ? null : plan.id(), t.id())).toList();
        var agents = plan == null ? List.<String>of() : catalog.agentAccess().stream().filter(a -> a.planId().equals(plan.id())
                && a.state() == AccessState.AVAILABLE && evidence.confirmed(a.evidenceId())).map(CatalogManifest.PlanAgentAccess::agentId).sorted().distinct().toList();
        var models = plan == null ? List.<String>of() : catalog.modelAccess().stream().filter(a -> a.planId().equals(plan.id())
                && a.state() == AccessState.AVAILABLE && evidence.confirmed(a.evidenceId())).map(CatalogManifest.PlanModelAccess::modelId).sorted().distinct().toList();
        var pools = offer == null ? List.<String>of() : catalog.pools().stream().filter(pool -> pool.offerId().equals(offer.id())).map(CatalogManifest.QuotaPool::id).sorted().distinct().toList();
        if (!agents.isEmpty()) reasons.add(reason(Code.INCLUDED_AGENT, "Included agents are part of the plan, not additional purchases", catalog.agentAccess().stream().filter(a -> a.planId().equals(plan.id()) && agents.contains(a.agentId())).map(CatalogManifest.PlanAgentAccess::evidenceId).toArray(String[]::new)));
        if (!models.isEmpty()) reasons.add(reason(Code.INCLUDED_MODEL, "Model access does not establish product quality", catalog.modelAccess().stream().filter(a -> a.planId().equals(plan.id()) && models.contains(a.modelId())).map(CatalogManifest.PlanModelAccess::evidenceId).toArray(String[]::new)));
        if (!pools.isEmpty()) reasons.add(reason(Code.SHARED_QUOTA, "Each quota pool is counted once across its consumers", catalog.pools().stream().filter(pool -> pools.contains(pool.id())).map(CatalogManifest.QuotaPool::evidenceId).toArray(String[]::new)));
        return new Candidate(id, plan, offer, existing, mandatory, status, costs, states, reasons, agents, models, pools);
    }
}
