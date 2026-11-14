package com.aicompass.compass.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static com.aicompass.compass.catalog.CatalogManifest.*;

public final class CatalogViews {
    private CatalogViews() {}
    public record Meta(@Schema(nullable = true) UUID versionId, long revision,
            @Schema(nullable = true) String datasetKind, @Schema(nullable = true) Instant asOf,
            @Schema(allowableValues = {"EMPTY", "AVAILABLE"}) String status,
            List<Region> regions, List<UserNeed> needs, List<Capability> capabilities,
            long productCount, long unknownClaims, long staleClaims) {}
    public record Page<T>(@Schema(nullable = true) UUID versionId, List<T> items, long total, int page, int size) {}
    public record Evidence(String id, @Schema(nullable = true) String sourceId, String claimType, String status, String effectiveStatus,
            @Schema(nullable = true) Instant verifiedAt, @Schema(nullable = true) String observedRegionId,
            String channel, String summary, String conditions) {}
    public record Source(String id, String url, String publisher, String kind) {}
    public record Offer(PriceOffer terms, @Schema(nullable = true) String monthlyEquivalent,
            @Schema(nullable = true) String upfront, @Schema(nullable = true) String allInMonthly,
            @Schema(nullable = true) String allInUpfront) {}
    public record Detail(UUID versionId, Product product, List<SubscriptionPlan> plans,
            List<Offer> offers, List<CapabilityAssertion> assertions, List<Capability> capabilities,
            List<PlanModelAccess> modelAccess, List<AiModel> models, List<PlanAgentAccess> agentAccess,
            List<Agent> agents, List<QuotaPool> quotaPools, List<UsageLimit> usageLimits,
            List<EligibilityRule> eligibilityRules, List<Region> regions,
            List<Evidence> evidence, List<Source> sources) {}
}
