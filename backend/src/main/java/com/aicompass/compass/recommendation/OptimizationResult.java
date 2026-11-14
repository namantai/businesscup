package com.aicompass.compass.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

public record OptimizationResult(UUID catalogVersion,String optimizerVersion,Instant asOf,int horizonMonths,
        String currency,RecommendationProfile profile,Map<String,SubscriptionTerms> subscriptions,String decision,Scenario current,List<Scenario> alternatives,List<Overlap> overlaps,
        RecommendationResult recommendations,List<RecommendationResult.FxUsed> fxSnapshots,List<String> limitations) {
    public record Charge(String subscriptionId,LocalDate date,String billingZone,
                         @Schema(nullable=true) String amount,boolean unavoidable) {}
    public record Timeline(@Schema(nullable=true) String paidAmount,
            @Schema(nullable=true) String cashDueNow,@Schema(nullable=true) String recurringMonthly,
            @Schema(nullable=true) String horizonTotal,@Schema(nullable=true) String mandatoryTotal,
            @Schema(nullable=true) String transitionCost,@Schema(nullable=true) LocalDate nextPayment,
            List<Charge> charges,List<String> unknowns) {}
    public record Scenario(String id,String label,List<String> subscriptionIds,List<RecommendationResult.Item> items,
            Timeline costs,List<RecommendationResult.Coverage> coverage,boolean requirementsMet,
            List<String> lostFunctions,List<RecommendationResult.PortfolioAction> actions,
            @Schema(nullable=true) String potentialSavings,@Schema(nullable=true) String confirmedSavings,
            String savingsStatus,List<String> conditions) {}
    public record Overlap(String leftId,String rightId,String status,List<String> capabilities,List<String> evidenceIds,String explanation) {}
}
