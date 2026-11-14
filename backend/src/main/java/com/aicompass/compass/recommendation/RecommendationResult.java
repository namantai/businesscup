package com.aicompass.compass.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.aicompass.compass.catalog.CatalogViews;
import static com.aicompass.compass.catalog.CatalogTypes.CapabilityState;

public record RecommendationResult(String requestId, @Schema(nullable = true) UUID catalogVersion, String datasetKind,
        String engineVersion, String policyVersion, Instant asOf, boolean searchComplete,
        Search search, String status, List<Bundle> bundles, List<Bundle> conditionalAlternatives,
        List<Unmet> unmetRequirements, List<Reason> warnings, List<CandidateAssessment> assessments,
        List<CatalogViews.Evidence> evidence, List<CatalogViews.Source> sources, List<FxUsed> fxSnapshots) {
    public RecommendationResult withRequestId(String id) {
        return new RecommendationResult(id, catalogVersion, datasetKind, engineVersion, policyVersion, asOf,
                searchComplete, search, status, bundles, conditionalAlternatives, unmetRequirements, warnings,
                assessments, evidence, sources, fxSnapshots);
    }
    public enum Eligibility { ELIGIBLE, CONDITIONAL, UNKNOWN, INELIGIBLE }
    public enum Strategy { ECONOMY, BALANCED, EXPANDED }
    public enum Confidence { CONFIRMED_COMPONENTS, ESTIMATED, INCOMPLETE }
    public enum Action { KEEP, ADD, CANCEL_AT_RENEWAL, REVIEW }
    public enum Code {
        WITHIN_BUDGET, PRICE_INCOMPLETE, ANNUAL_UPFRONT_REQUIRED, STUDENT_VERIFICATION_REQUIRED,
        REGION_UNKNOWN, REGION_INELIGIBLE, MISSING_REQUIRED_FEATURE, SHARED_QUOTA, TRIAL_NOT_RECURRING,
        KEEP_EXISTING, INCLUDED_AGENT, INCLUDED_MODEL, UNKNOWN_QUALITY, STALE_EVIDENCE, BUDGET_CONFLICT,
        UPFRONT_CONFLICT, NO_DISTINCT_ALTERNATIVE, LIMITED_CATALOG, SEARCH_TRUNCATED,
        NO_PUBLISHED_CATALOG, ELIGIBILITY_UNKNOWN, PLATFORM_UNKNOWN, PRIVACY_UNKNOWN,
        LANGUAGE_UNKNOWN, USAGE_UNKNOWN, USAGE_EXCEEDED, FX_MISSING, CONDITION_ACCEPTED,
        CONDITIONAL_EVIDENCE, CONFLICTED_EVIDENCE, RETAINED_CONFLICT, INCOMPATIBLE_PLAN,
        NOT_ACTIVE, PURCHASE_CHANNEL_MISMATCH, NO_MATCH, BASELINE_INCOMPLETE, USER_SUPPLIED_COST,
        CANCEL_AT_RENEWAL, COMPASS_FEE, UNKNOWN_CAPABILITY, API_NOT_APPLICATION
    }
    public record Reason(Code code, String detail, List<String> evidenceIds) {}
    public record Search(int catalogProducts, int candidateCount, int consideredCandidates,
                         long visitedCombinations, long feasibleCombinations, long dominatedCombinations,
                         String limitation) {}
    public record Costs(String currency, @Schema(nullable = true) String cashDueNow,
            @Schema(nullable = true) String upfront, @Schema(nullable = true) String recurringMonthly,
            @Schema(nullable = true) String horizonTotal, @Schema(nullable = true) String futureMonthly,
            @Schema(nullable = true) String comparableBaselineMonthly, @Schema(nullable = true) String potentialNetMonthlySavings,
            @Schema(nullable = true) String compassFeeMonthly, Confidence confidence, boolean confirmedBudgetFit) {}
    public record Item(String id, @Schema(nullable = true) String productId, @Schema(nullable = true) String planId,
            @Schema(nullable = true) String offerId, Action action, String originalCurrency,
            @Schema(nullable = true) String originalCharge, String billingPeriod, @Schema(nullable = true) java.time.LocalDate nextRenewal,
            List<String> includedAgentIds, List<String> modelIds, List<String> sharedPoolIds, List<String> evidenceIds) {}
    public record Coverage(String capabilityId, int weight, boolean required, CapabilityState state,
            boolean confirmed, boolean acceptedForRequirement, List<String> evidenceIds) {}
    public record Unmet(String capabilityId, CapabilityState state, Code reason) {}
    public record Bundle(String id, List<Strategy> strategies, Eligibility eligibility, List<Item> items,
            Costs costs, List<Coverage> coverage, String usageFit, List<Reason> factualReasons, List<Reason> policyReasons,
            List<String> conditions, List<PortfolioAction> portfolioActions) {}
    public record PortfolioAction(String subscriptionId, Action action, String explanation) {}
    public record CandidateAssessment(String id, Eligibility eligibility, List<Reason> reasons) {}
    public record FxUsed(String id, String baseCurrency, String quoteCurrency, String rate, Instant observedAt, String evidenceId) {}
}
