package com.aicompass.compass.recommendation;

import java.util.*;
import com.aicompass.compass.catalog.CatalogManifest;
import static com.aicompass.compass.recommendation.RecommendationResult.*;

/** Solver input, prepared once. Mutable masks remain private to this calculation. */
final class Candidate {
    final String id;
    final CatalogManifest.SubscriptionPlan plan;
    final CatalogManifest.PriceOffer offer;
    final RecommendationProfile.ExistingSubscription existing;
    final boolean mandatory;
    final Eligibility eligibility;
    final CostCalculator.Amounts costs;
    final List<CapabilityCoverage.State> states;
    final BitSet confirmed = new BitSet(), accepted = new BitSet();
    final List<Reason> reasons;
    final List<String> agents, models, pools;
    Candidate(String id, CatalogManifest.SubscriptionPlan plan, CatalogManifest.PriceOffer offer,
            RecommendationProfile.ExistingSubscription existing, boolean mandatory, Eligibility eligibility,
            CostCalculator.Amounts costs, List<CapabilityCoverage.State> states, List<Reason> reasons,
            List<String> agents, List<String> models, List<String> pools) {
        this.id = id; this.plan = plan; this.offer = offer; this.existing = existing; this.mandatory = mandatory;
        this.eligibility = eligibility; this.costs = costs; this.states = states; this.reasons = List.copyOf(reasons);
        this.agents = agents; this.models = models; this.pools = pools;
        for (int i = 0; i < states.size(); i++) { if (states.get(i).confirmed()) confirmed.set(i); if (states.get(i).accepted()) accepted.set(i); }
    }
    String productId() { return plan == null ? "custom:" + id : plan.productId(); }
    String conflictGroup() { return plan == null || plan.exclusivityGroupId() == null ? null : plan.productId() + ":" + plan.exclusivityGroupId(); }
}
