package com.aicompass.compass.portfolio;

import com.aicompass.compass.recommendation.*;
import com.aicompass.compass.identity.AuthDtos.Account;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class PortfolioDtos {
    private PortfolioDtos() {}
    public record ProfileWrite(@NotBlank @Size(max=100) String name, @NotNull RecommendationProfile profile, @Min(0) long revision) {}
    public record SavedProfile(UUID id, String name, RecommendationProfile profile, long revision, Instant createdAt, Instant updatedAt) {}
    public record SnapshotWrite(@NotNull RecommendationProfile profile, @NotNull UUID catalogVersion,
            @NotNull Instant asOf, @NotBlank String engineVersion, @NotBlank String policyVersion) {}
    public record SavedRecommendation(UUID id, RecommendationProfile profile, RecommendationResult result, Instant savedAt) {}
    public record StackWrite(@NotNull UUID snapshotId, @NotBlank @Size(max=200) String bundleId) {}
    public record SavedStack(UUID id, UUID snapshotId, String bundleId, Instant createdAt) {}
    public record DataExport(Account account, List<SavedProfile> profiles, List<SavedRecommendation> recommendations, List<SavedStack> stacks) {}
}
