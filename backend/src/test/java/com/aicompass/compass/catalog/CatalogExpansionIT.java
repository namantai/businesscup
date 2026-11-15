package com.aicompass.compass.catalog;

import java.time.Clock;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.node.ArrayNode;
import static com.aicompass.compass.catalog.CatalogTestData.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "compass.catalog.allow-demo=true")
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class CatalogExpansionIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(DockerImageName.parse(
            "postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withDatabaseName("compass_test")
            .withLabel("com.aicompass.scope", "ce-integration")
            .withCreateContainerCmdModifier(command -> {
                HostConfig config = command.getHostConfig();
                config.withPortBindings(PortBinding.parse("127.0.0.1::5432"));
                command.withHostConfig(config);
            });

    static {
        DATABASE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(DATABASE::stop));
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @AfterAll
    static void stopDatabase() {
        DATABASE.stop();
    }

    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock testClock() { return Clock.fixed(java.time.Instant.parse("2026-11-14T23:43:01Z"), java.time.ZoneOffset.UTC); }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogImportService importer;
    @Autowired CatalogPublicationService publication;
    @Autowired CatalogReadService catalog;
    @org.springframework.boot.test.web.server.LocalServerPort int port;

    private UUID load(String file) throws Exception {
        try (var in = getClass().getResourceAsStream("/data/catalog/" + file)) {
            return importer.importManifest(new String(java.util.Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    private void review(UUID id) {
        publication.review(id, jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id = ?", String.class, id), "ce-isolated-review");
    }
    private java.net.http.HttpResponse<String> get(String path) throws Exception {
        return java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://127.0.0.1:" + port + "/api/v1/catalog/" + path)).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    @Test @Order(1) void realExpansionImportsReviewsPublishesAndRollsBackWithoutRewritingR2() throws Exception {
        UUID a = load("catalog-2026-11-14-r2.json"); review(a);
        publication.activate(a, publication.current().revision(), false, "ce-test", "Original reviewed r2 in isolated DB");
        String hash = jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?", String.class, a);
        UUID b = load("catalog-2026-11-14-ce1.json");
        assertEquals(404, get("products?version=" + b).statusCode());
        assertThrows(IllegalArgumentException.class, () -> publication.activate(b, publication.current().revision(), false, "test", "Unreviewed"));
        review(b); assertEquals(404, get("products?version=" + b).statusCode());
        publication.activate(b, publication.current().revision(), false, "ce-test", "Reviewed expansion in isolated DB");
        assertEquals(23, catalog.meta().productCount());
        assertEquals(3, TREE.readTree(get("products?version=" + a).body()).get("total").asInt());
        assertEquals(hash, jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?", String.class, a));
        publication.activate(a, publication.current().revision(), true, "ce-test", "Test rollback r2");
        assertEquals(3, catalog.meta().productCount());
        assertEquals(23, TREE.readTree(get("products?version=" + b).body()).get("total").asInt());
        publication.activate(b, publication.current().revision(), true, "ce-test", "Return expansion for filter tests");
    }
    @Test @Order(2) void filtersUseSameOfferAndEvidenceBeforePagination() throws Exception {
        var us = TREE.readTree(get("products?priceKind=FREE&regionId=us&size=2").body());
        assertEquals(2, us.get("items").size()); assertTrue(us.get("total").asInt() >= 8);
        assertEquals(0, TREE.readTree(get("products?priceKind=FREE&regionId=kz").body()).get("total").asInt());
        assertTrue(TREE.readTree(get("products?priceKind=UNKNOWN&regionId=kz").body()).get("total").asInt() >= 12);
        assertEquals(1, TREE.readTree(get("products?priceKind=FIXED&regionId=cn&currency=CNY").body()).get("total").asInt());
        assertEquals(0, TREE.readTree(get("products?priceKind=FIXED&regionId=cn&currency=USD").body()).get("total").asInt());
        assertEquals(400, get("products?priceKind=GUESS").statusCode());
        assertTrue(TREE.readTree(get("products?capabilityId=video-generation&q=Luma").body()).get("total").asInt() > 0);
    }
    @Test @Order(3) void freeFilterRejectsStaleAndConflictedPriceEvidence() throws Exception {
        // Immutable published data cannot be changed to create this regression case.
        var raw = new String(getClass().getResourceAsStream("/data/catalog/catalog-2026-11-14-ce1.json").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        var tree = (tools.jackson.databind.node.ObjectNode) TREE.readTree(raw);
        tree.put("versionId", UUID.randomUUID().toString());
        for (var e : tree.get("evidence")) {
            if (e.get("id").asString().equals("ce-gemini-free-us-month-price")) ((tools.jackson.databind.node.ObjectNode)e).put("status", "CONFLICTED");
            if (e.get("id").asString().equals("ce-suno-free-us-month-price")) ((tools.jackson.databind.node.ObjectNode)e).put("verifiedAt", "2026-11-01T00:00:00Z");
        }
        UUID id = importer.importManifest(TREE.writeValueAsString(tree)); review(id);
        publication.activate(id, publication.current().revision(), false, "ce-test", "Isolated adverse evidence fixture, never dev");
        assertEquals(0, TREE.readTree(get("products?priceKind=FREE&q=Gemini").body()).get("total").asInt());
        assertEquals(0, TREE.readTree(get("products?priceKind=FREE&q=Suno").body()).get("total").asInt());
        assertEquals(1, TREE.readTree(get("products?q=Gemini").body()).get("total").asInt());
    }
}
