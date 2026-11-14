package com.aicompass.compass.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecommendationProfile(
        @Min(1) @Max(1) int profileSchemaVersion,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String country,
        @NotBlank @Pattern(regexp = "[a-z]{2,3}(-[A-Z]{2})?") String interfaceLanguage,
        @NotNull @Size(max = 5) List<@NotBlank @Pattern(regexp = "[a-z]{2,3}(-[A-Z]{2})?") String> outputLanguages,
        @NotNull @Size(min = 1, max = 12) List<@NotNull @Valid Need> needs,
        @NotNull @Valid Budget budget,
        boolean allowAnnualBilling,
        @Min(1) @Max(4) int maxTools,
        @Min(1) @Max(24) int horizonMonths,
        @NotNull Priority priority,
        @NotNull StudentStatus studentStatus,
        @NotNull PortfolioMode portfolioMode,
        @NotBlank @Size(max = 40) String purchaseChannel,
        @NotNull @Valid Requirements requirements,
        @NotNull @Size(max = 8) List<@NotNull @Valid ExistingSubscription> existingSubscriptions,
        @NotNull @Size(max = 32) List<@NotBlank @Size(max = 80) String> acceptedConditionEvidenceIds,
        @Schema(nullable = true) Instant asOf) {
    public enum Intensity { OCCASIONAL, WEEKLY, DAILY }
    public enum Priority { ECONOMY, BALANCE, CAPABILITIES }
    public enum StudentStatus { NONE, SELF_DECLARED, USER_REPORTED_PROVIDER_VERIFIED }
    public enum PortfolioMode { AUGMENT, REVIEW }
    public enum Privacy { NO_STRICT_REQUIREMENT, LOCAL_ONLY }
    public enum BudgetScope { ALL_AI }
    public enum Period { MONTH, YEAR }
    public record Need(@NotBlank @Size(max = 80) String id, @Min(1) @Max(5) int weight,
                       boolean required, @NotNull Intensity intensity) {}
    public record Budget(@NotBlank String currency,
                         @NotBlank String monthlyMax, @NotBlank String upfrontMax, @NotNull BudgetScope scope) {}
    public record Requirements(@NotNull @Size(max = 8) List<@NotBlank @Size(max = 40) String> platforms,
            @NotNull Privacy privacy,
            @NotNull @Size(max = 32) List<@NotNull @Valid Feature> features,
            @NotNull @Size(max = 16) List<@NotNull @Valid Usage> usage) {}
    public record Feature(@NotBlank @Size(max = 80) String capabilityId, @Min(1) @Max(5) int weight, boolean required) {}
    public record Usage(@NotBlank @Size(max = 80) String capabilityId, @NotBlank @Size(max = 80) String mode,
                        @NotBlank String monthlyQuantity, @NotBlank @Size(max = 80) String unit, boolean required) {}
    public record ExistingSubscription(@NotBlank @Size(max = 80) String id,
            @Schema(nullable = true) @Size(max = 80) String planId,
            @Schema(nullable = true) @Size(max = 80) String offerId,
            @NotBlank String currency, @Schema(nullable = true) String amount,
            @NotNull Period billingPeriod, @NotNull LocalDate renewalAnchor, @NotNull LocalDate nextRenewal,
            @NotBlank String billingZone, boolean mustKeep) {}
}
