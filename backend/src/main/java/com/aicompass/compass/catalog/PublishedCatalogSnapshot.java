package com.aicompass.compass.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static com.aicompass.compass.catalog.CatalogManifest.*;

/** Immutable value boundary for a single database snapshot. No operator metadata crosses it. */
public record PublishedCatalogSnapshot(UUID version, String datasetKind, Instant publishedAsOf,
        List<Product> products, List<SubscriptionPlan> plans, List<PriceOffer> offers,
        List<Region> regions, List<UserNeed> needs, List<NeedCapability> mappings,
        List<Capability> capabilities, List<CapabilityAssertion> assertions,
        List<PlanAgentAccess> agentAccess, List<Agent> agents,
        List<PlanModelAccess> modelAccess, List<AiModel> models,
        List<QuotaPool> pools, List<UsageLimit> limits, List<EligibilityRule> eligibility,
        List<FxSnapshot> fx, List<CatalogViews.Evidence> evidence, List<CatalogViews.Source> sources) {
    public PublishedCatalogSnapshot {
        products = List.copyOf(products); plans = List.copyOf(plans); offers = List.copyOf(offers);
        regions = List.copyOf(regions); needs = List.copyOf(needs); mappings = List.copyOf(mappings);
        capabilities = List.copyOf(capabilities); assertions = List.copyOf(assertions);
        agentAccess = List.copyOf(agentAccess); agents = List.copyOf(agents);
        modelAccess = List.copyOf(modelAccess); models = List.copyOf(models);
        pools = List.copyOf(pools); limits = List.copyOf(limits); eligibility = List.copyOf(eligibility);
        fx = List.copyOf(fx); evidence = List.copyOf(evidence); sources = List.copyOf(sources);
    }
}
