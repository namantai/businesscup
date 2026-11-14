package com.aicompass.compass.catalog;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String officialDomain) {}

    public record Region(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record EvidenceSource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^https://.+") String url,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String publisher,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull SourceKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String usageRights) {}

    public record Evidence(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ClaimType claimType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull EvidenceStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant verifiedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String observedRegionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String channel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String reviewer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String method,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions) {}

    public record Product(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ProductKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ProductState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String metadataEvidenceId) {}

    public record ModelFamily(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record AiModel(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String familyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String exactVersion) {}

    public record Agent(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String providerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String executionEnvironment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String metadataEvidenceId) {}

    public record Capability(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull CapabilityCategory category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record UserNeed(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record NeedCapability(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String needId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId) {}

    public record ExclusivityGroup(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String name) {}

    public record SubscriptionPlan(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String tier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull AccessType accessType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String exclusivityGroupId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant activeFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant activeUntil) {}

    public record PriceOffer(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String regionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String channel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull PriceKind priceKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull BillingPeriod billingPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String upfrontAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) String usageUnit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TaxStatus taxStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String additionalTax,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ValueStatus feeStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String additionalFee,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Min(1) @Max(3660) Integer trialDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant validFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant validUntil,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String priceEvidenceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String taxEvidenceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String feeEvidenceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String regionEvidenceId) {}

    public record PlanModelAccess(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String modelId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String mode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull AccessState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record PlanAgentAccess(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String agentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull AccessState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record CapabilityAssertion(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String productId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String planId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull CapabilityState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record QuotaPool(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull LimitState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String unit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ResetPeriod resetPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record UsageLimit(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String capabilityId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String poolId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String mode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String unitsPerUse) {}

    public record EligibilityRule(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String offerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull EligibilityKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull EligibilityState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String requirement,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String conditions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record BenchmarkObservation(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String modelId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String task,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String metric,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^-?(0|[1-9][0-9]{0,11})(\\.[0-9]{1,6})?$") String value,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String unit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) String methodology,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant observedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

    public record FxSnapshot(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String baseCurrency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^[A-Z]{3}$") String quoteCurrency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 2000) @Pattern(regexp = "^(0|[1-9][0-9]{0,11})(\\.[0-9]{1,12})?$") String rate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant observedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 80) @Pattern(regexp = "^[a-z][a-z0-9-]{0,79}$") String evidenceId) {}

}
