package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static com.aicompass.compass.recommendation.RecommendationProfile.*;

final class EngineFixtures {
    static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    static final JsonMapper JSON = JsonMapper.builder().build();
    static String manifest() {
        try (var stream = EngineFixtures.class.getResourceAsStream("/data/fixtures/recommendation-demo.json")) {
            return new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    static CatalogManifest data(Consumer<ObjectNode> mutation) {
        var tree = (ObjectNode) JSON.readTree(manifest()); mutation.accept(tree);
        return new CatalogJson().read(JSON.writeValueAsString(tree));
    }
    static PublishedCatalogSnapshot snapshot() { return snapshot(data(t -> {})); }
    static PublishedCatalogSnapshot snapshot(CatalogManifest m) {
        return new PublishedCatalogSnapshot(m.versionId(), m.datasetKind().name(), m.asOf(), m.products(), m.subscriptionPlans(),
                m.priceOffers(), m.regions(), m.userNeeds(), m.needCapabilities(), m.capabilities(), m.capabilityAssertions(),
                m.planAgentAccess(), m.agents(), m.planModelAccess(), m.aiModels(), m.quotaPools(), m.usageLimits(), m.eligibilityRules(), m.fxSnapshots(),
                m.evidence().stream().map(e -> new CatalogViews.Evidence(e.id(),e.sourceId(),e.claimType().name(),e.status().name(),e.status().name(),e.verifiedAt(),e.observedRegionId(),e.channel(),e.summary(),e.conditions())).toList(),
                m.evidenceSources().stream().map(s -> new CatalogViews.Source(s.id(),s.url(),s.publisher(),s.kind().name())).toList());
    }
    static ObjectNode row(ObjectNode tree, String table, String id) {
        for (var value : tree.get(table)) if (value.get("id").asString().equals(id)) return (ObjectNode)value;
        throw new IllegalArgumentException(id);
    }
    static RecommendationProfile student() {
        return new RecommendationProfile(1,"KZ","ru",List.of("ru","en"),
                List.of(new Need("study",4,true,Intensity.WEEKLY),new Need("coding",5,true,Intensity.DAILY),new Need("presentations",3,true,Intensity.WEEKLY)),
                new Budget("USD","20","30",BudgetScope.ALL_AI),false,4,12,Priority.BALANCE,StudentStatus.SELF_DECLARED,PortfolioMode.AUGMENT,"WEB",
                new Requirements(List.of("LINUX"),Privacy.NO_STRICT_REQUIREMENT,List.of(),List.of()), List.of(),List.of(),NOW);
    }
    static RecommendationProfile editor() {
        return change(student(), t -> {
            var needs=t.putArray("needs");
            needs.addObject().put("id","images").put("weight",3).put("required",true).put("intensity","WEEKLY");
            needs.addObject().put("id","video").put("weight",5).put("required",true).put("intensity","WEEKLY");
            ((ObjectNode)t.get("requirements")).putArray("platforms").add("WEB");
        });
    }
    static RecommendationProfile change(RecommendationProfile profile, Consumer<ObjectNode> mutation) {
        var tree=(ObjectNode)JSON.valueToTree(profile); mutation.accept(tree);
        return JSON.treeToValue(tree,RecommendationProfile.class);
    }
    static RecommendationResult run(RecommendationProfile profile) { return new RecommendationEngine().calculate(profile,snapshot(),NOW); }
    static List<String> ids(RecommendationResult.Bundle b) { return b.items().stream().map(RecommendationResult.Item::productId).sorted().toList(); }
}
