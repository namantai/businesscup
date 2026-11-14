package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class EvidenceAssessment {
    private final Map<String, CatalogViews.Evidence> entries;
    private final boolean demo;
    private final Instant asOf;
    EvidenceAssessment(PublishedCatalogSnapshot snapshot, Instant asOf) {
        entries = snapshot.evidence().stream().collect(Collectors.toMap(CatalogViews.Evidence::id, Function.identity()));
        demo = snapshot.datasetKind().equals("DEMO"); this.asOf = asOf;
    }
    String status(String id) {
        var e = entries.get(id);
        if (e == null || e.verifiedAt() == null || e.verifiedAt().isAfter(asOf)) return "UNKNOWN";
        if (e.status().equals("DEMO")) return demo ? "DEMO" : "UNKNOWN";
        Duration age = switch (e.claimType()) {
            case "PRICE", "TAX", "FEES" -> Duration.ofDays(7);
            case "MODEL_ACCESS", "AGENT_ACCESS", "USAGE_LIMIT" -> Duration.ofDays(14);
            case "FX" -> Duration.ofHours(24);
            default -> Duration.ofDays(30);
        };
        if (Set.of("VERIFIED_SOURCE", "CONDITIONAL").contains(e.status()) && !asOf.isBefore(e.verifiedAt().plus(age))) return "STALE";
        return e.status();
    }
    boolean confirmed(String id) { return Set.of("VERIFIED_SOURCE", "DEMO").contains(status(id)); }
    boolean accepted(String id, RecommendationProfile profile) {
        return status(id).equals("CONDITIONAL") && profile.acceptedConditionEvidenceIds().contains(id);
    }
    CatalogViews.Evidence get(String id) { return entries.get(id); }
    List<CatalogViews.Evidence> publicEvidence() {
        return entries.values().stream().sorted(Comparator.comparing(CatalogViews.Evidence::id))
                .map(e -> new CatalogViews.Evidence(e.id(), e.sourceId(), e.claimType(), e.status(), status(e.id()),
                        e.verifiedAt(), e.observedRegionId(), e.channel(), e.summary(), e.conditions())).toList();
    }
}
