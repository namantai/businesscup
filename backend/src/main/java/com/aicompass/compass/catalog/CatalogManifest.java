package com.aicompass.compass.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static com.aicompass.compass.catalog.CatalogTypes.*;

public record CatalogManifest(
    @Min(1) @Max(1) int schemaVersion,
    @NotNull UUID versionId,
    @NotNull DatasetKind datasetKind,
    @NotNull Instant asOf,
    @NotBlank String reviewer,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Provider> providers,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Region> regions,
    @NotNull @Size(max = 10000) List<@NotNull @Valid EvidenceSource> evidenceSources,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Evidence> evidence,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Product> products,
    @NotNull @Size(max = 10000) List<@NotNull @Valid ModelFamily> modelFamilies,
    @NotNull @Size(max = 10000) List<@NotNull @Valid AiModel> aiModels,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Agent> agents,
    @NotNull @Size(max = 10000) List<@NotNull @Valid Capability> capabilities,
    @NotNull @Size(max = 10000) List<@NotNull @Valid UserNeed> userNeeds,
    @NotNull @Size(max = 10000) List<@NotNull @Valid NeedCapability> needCapabilities,
    @NotNull @Size(max = 10000) List<@NotNull @Valid ExclusivityGroup> exclusivityGroups,
    @NotNull @Size(max = 10000) List<@NotNull @Valid SubscriptionPlan> subscriptionPlans,
    @NotNull @Size(max = 10000) List<@NotNull @Valid PriceOffer> priceOffers,
    @NotNull @Size(max = 10000) List<@NotNull @Valid PlanModelAccess> planModelAccess,
    @NotNull @Size(max = 10000) List<@NotNull @Valid PlanAgentAccess> planAgentAccess,
    @NotNull @Size(max = 10000) List<@NotNull @Valid CapabilityAssertion> capabilityAssertions,
    @NotNull @Size(max = 10000) List<@NotNull @Valid QuotaPool> quotaPools,
    @NotNull @Size(max = 10000) List<@NotNull @Valid UsageLimit> usageLimits,
    @NotNull @Size(max = 10000) List<@NotNull @Valid EligibilityRule> eligibilityRules,
    @NotNull @Size(max = 10000) List<@NotNull @Valid BenchmarkObservation> benchmarkObservations,
    @NotNull @Size(max = 10000) List<@NotNull @Valid FxSnapshot> fxSnapshots) {
    public record Provider(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 2000) String name,
        @NotBlank @Size(min = 1, max = 2000) String officialDomain) {}

    public record Region(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
        @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record EvidenceSource(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^https://.+") String url,
        @NotBlank @Size(min = 1, max = 2000) String publisher,
        @NotNull SourceKind kind,
        @NotBlank @Size(min = 1, max = 2000) String usageRights) {}

    public record Evidence(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String sourceId,
        @NotNull ClaimType claimType,
        @NotNull EvidenceStatus status,
         Instant verifiedAt,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String observedRegionId,
        @NotBlank @Size(min = 1, max = 2000) String channel,
        @NotBlank @Size(min = 1, max = 2000) String summary,
        @NotBlank @Size(min = 1, max = 2000) String reviewer,
        @NotBlank @Size(min = 1, max = 2000) String method,
        @NotBlank @Size(min = 1, max = 2000) String conditions) {}

    public record Product(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String slug,
        @NotBlank @Size(min = 1, max = 2000) String name,
        @NotNull ProductKind kind,
        @NotNull ProductState state,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String metadataEvidenceId) {}

    public record ModelFamily(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record AiModel(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String familyId,
        @NotBlank @Size(min = 1, max = 2000) String name,
        @NotBlank @Size(min = 1, max = 2000) String exactVersion) {}

    public record Agent(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @NotBlank @Size(min = 1, max = 2000) String name,
        @NotBlank @Size(min = 1, max = 2000) String executionEnvironment,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String metadataEvidenceId) {}

    public record Capability(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotNull CapabilityCategory category,
        @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record UserNeed(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record NeedCapability(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String needId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId) {}

    public record ExclusivityGroup(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record SubscriptionPlan(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @NotBlank @Size(min = 1, max = 2000) String tier,
        @NotNull AccessType accessType,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String exclusivityGroupId,
        @NotNull Instant activeFrom,
         Instant activeUntil) {}

    public record PriceOffer(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String regionId,
        @NotBlank @Size(min = 1, max = 2000) String channel,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull PriceKind priceKind,
        @NotNull BillingPeriod billingPeriod,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String amount,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String upfrontAmount,
        @Size(min = 1, max = 2000) String usageUnit,
        @NotNull TaxStatus taxStatus,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String additionalTax,
        @NotNull ValueStatus feeStatus,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String additionalFee,
        @Min(1) @Max(3660) Integer trialDays,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotNull Instant validFrom,
         Instant validUntil,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String priceEvidenceId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String taxEvidenceId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String feeEvidenceId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String regionEvidenceId) {}

    public record PlanModelAccess(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String modelId,
        @NotBlank @Size(min = 1, max = 2000) String mode,
        @NotNull AccessState state,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record PlanAgentAccess(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String agentId,
        @NotNull AccessState state,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record CapabilityAssertion(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId,
        @NotNull CapabilityState state,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record QuotaPool(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @NotNull LimitState state,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String quantity,
        @NotBlank @Size(min = 1, max = 2000) String unit,
        @NotNull ResetPeriod resetPeriod,
         Instant expiresAt,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record UsageLimit(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String poolId,
        @NotBlank @Size(min = 1, max = 2000) String mode,
        @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String unitsPerUse) {}

    public record EligibilityRule(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @NotNull EligibilityKind kind,
        @NotNull EligibilityState state,
        @NotBlank @Size(min = 1, max = 2000) String requirement,
        @NotBlank @Size(min = 1, max = 2000) String conditions,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record BenchmarkObservation(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String modelId,
        @NotBlank @Size(min = 1, max = 2000) String task,
        @NotBlank @Size(min = 1, max = 2000) String metric,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^-?(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String value,
        @NotBlank @Size(min = 1, max = 2000) String unit,
        @NotBlank @Size(min = 1, max = 2000) String methodology,
        @NotNull Instant observedAt,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record FxSnapshot(
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String baseCurrency,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String quoteCurrency,
        @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,12})?$") String rate,
        @NotNull Instant observedAt,
        @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

}
