package com.aicompass.compass.portfolio;

import com.aicompass.compass.recommendation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class SubscriptionDtos {
    private SubscriptionDtos() {}
    public record SubscriptionWrite(@NotNull @Valid SubscriptionTerms terms,@Min(0) long revision) {}
    public record SubscriptionEntry(UUID id,SubscriptionTerms terms,long revision,Instant createdAt,Instant updatedAt) {}
    public record OptimizationRequest(@NotNull RecommendationProfile profile,@NotNull UUID catalogVersion,
                                     @NotNull Map<UUID,Long> revisions) {}
}
