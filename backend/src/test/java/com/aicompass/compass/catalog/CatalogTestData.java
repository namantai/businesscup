package com.aicompass.compass.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class CatalogTestData {
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC);
    public static final JsonMapper TREE = JsonMapper.builder().build();
    public static String fixture() {
        try (var input = CatalogTestData.class.getResourceAsStream("/data/fixtures/catalog-demo.json")) {
            if (input == null) throw new IllegalStateException("Missing synthetic fixture");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    public static String mutated(Consumer<ObjectNode> mutation) {
        ObjectNode root = (ObjectNode) TREE.readTree(fixture());
        mutation.accept(root);
        return TREE.writeValueAsString(root);
    }
    public static ObjectNode row(ObjectNode root, String list, int index) {
        return (ObjectNode) root.get(list).get(index);
    }
    public static CatalogManifest.PriceOffer offer(String name) {
        return new CatalogJson().read(fixture()).priceOffers().stream()
                .filter(o -> o.id().equals("demo-offer-" + name)).findFirst().orElseThrow();
    }
}
