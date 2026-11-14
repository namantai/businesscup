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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "compass.catalog.allow-demo=true")
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class CatalogPublicationIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(DockerImageName.parse(
            "postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withDatabaseName("compass_test")
            .withLabel("com.aicompass.scope", "p03-integration")
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
        @Bean @Primary Clock testClock() { return CLOCK; }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogImportService importer;
    @Autowired CatalogPublicationService publication;
    @Autowired CatalogReadService catalog;
    @org.springframework.boot.test.web.server.LocalServerPort int port;

    private UUID imported() { return importer.importManifest(mutated(d -> d.put("versionId", UUID.randomUUID().toString()))); }
    private void review(UUID id) {
        publication.review(id, jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id = ?", String.class, id), "test-reviewer");
    }
    private void activate(UUID id) { review(id); publication.activate(id, publication.current().revision(), false, "test", "Reviewed synthetic test"); }
    private java.net.http.HttpResponse<String> request(String method, String path) throws Exception {
        return java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://127.0.0.1:" + port + "/api/v1/catalog/" + path))
                .method(method, java.net.http.HttpRequest.BodyPublishers.noBody()).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    @Test @Order(1) void emptyDatabaseIsHonest() throws Exception {
        assertEquals("EMPTY", catalog.meta().status());
        assertEquals(0, TREE.readTree(request("GET", "products").body()).get("total").asInt());
        assertEquals(404, request("GET", "products/absent").statusCode());
    }
    @Test void importedAndReviewedSnapshotsStayPrivateUntilActivation() throws Exception {
        UUID id = imported();
        assertEquals(404, request("GET", "products?version=" + id).statusCode());
        review(id);
        assertEquals(404, request("GET", "products?version=" + id).statusCode());
        assertThrows(IllegalArgumentException.class, () -> publication.review(id, "wrong-hash", "test"));
        assertThrows(IllegalArgumentException.class, () -> publication.activate(imported(), publication.current().revision(), false, "test", "unreviewed"));
    }
    @Test void publicationSwitchRollbackAndVersionPinnedReads() throws Exception {
        UUID first = imported(); activate(first);
        UUID second = imported(); activate(second);
        assertEquals(second, catalog.meta().versionId());
        assertEquals(first.toString(), TREE.readTree(request("GET", "products?version=" + first).body()).get("versionId").asString());
        publication.activate(first, publication.current().revision(), true, "test", "Rollback regression");
        assertEquals(first, catalog.meta().versionId());
        assertEquals(2, TREE.readTree(request("GET", "products?version=" + second).body()).get("total").asInt());
    }
    @Test void rollbackRequiresPreviouslyActivatedSnapshotAndIsAtomicOnFailure() {
        UUID active = imported(); activate(active);
        UUID next = imported(); review(next);
        var previous = publication.current();
        long events = jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication", Long.class);
        assertThrows(DataAccessException.class, () -> publication.activate(next, previous.revision(), true, "test", "Never public"));
        assertEquals(previous, publication.current());
        assertEquals(events, jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication", Long.class));
    }
    @Test void concurrentPublishHasOneWinnerAndRejectsStaleRevisionAfterRollback() throws Exception {
        UUID startId = imported(); activate(startId);
        UUID a = imported(); UUID b = imported(); review(a); review(b);
        var start = publication.current();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var go = new java.util.concurrent.CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(a,b).stream().map(id -> executor.submit(() -> {
                ready.countDown(); go.await();
                try { publication.activate(id, start.revision(), false, "test", "Concurrent activation"); return true; }
                catch (IllegalStateException expected) { return false; }
            })).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            int winners = 0; for (var task : tasks) if (task.get(20, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
        }
        assertEquals(start.revision() + 1, publication.current().revision());
        publication.activate(startId, publication.current().revision(), true, "test", "Return to A");
        assertThrows(IllegalStateException.class, () -> publication.activate(a, start.revision(), false, "test", "Stale ABA command"));
    }
    @Test void paginationFilteringSortingAndUnknownVersion() throws Exception {
        activate(imported());
        var page = TREE.readTree(request("GET", "products?size=1&sort=-name").body());
        assertEquals(2, page.get("total").asInt()); assertEquals(1, page.get("items").size());
        var next = TREE.readTree(request("GET", "products?size=1&page=1&sort=-name").body());
        assertNotEquals(page.get("items").get(0).get("id"), next.get("items").get(0).get("id"));
        assertEquals(0, TREE.readTree(request("GET", "products?q=not-present").body()).get("total").asInt());
        assertEquals(1, TREE.readTree(request("GET", "products?q=studio").body()).get("total").asInt());
        assertEquals(1, TREE.readTree(request("GET", "products?capabilityId=demo-coding").body()).get("total").asInt());
        assertEquals(400, request("GET", "products?size=101").statusCode());
        assertEquals(400, request("GET", "products?sort=reviewer").statusCode());
        assertEquals(400, request("GET", "products?kind=MODEL").statusCode());
        assertEquals(404, request("GET", "products?version=" + UUID.randomUUID()).statusCode());
        assertEquals(400, request("GET", "products?version=bad").statusCode());
    }
    @Test void allResourcesReadTypedDatabaseRowsAndDoNotLeakInternals() throws Exception {
        activate(imported());
        for (var resource : CatalogResource.values()) {
            var response = request("GET", resource.path);
            assertEquals(200, response.statusCode(), resource.path + ": " + response.body());
            assertFalse(response.body().contains("manifest_json"));
            assertFalse(response.body().contains("reviewer"));
        }
        for (String resource : List.of("evidence", "sources", "products/demo-studio")) {
            var response = request("GET", resource);
            assertEquals(200, response.statusCode(), response.body());
            assertFalse(response.body().contains("reviewer"));
            assertFalse(response.body().contains("method\""));
            assertFalse(response.body().contains("contentHash"));
        }
        assertEquals(404, request("GET", "catalog_version").statusCode());
    }
    @Test void preservesUnknownFreeRegionalAmountsAndFreshness() throws Exception {
        activate(imported());
        var body = TREE.readTree(request("GET", "offers?size=100").body());
        boolean unknown = false, free = false;
        for (var item : body.get("items")) {
            var terms = item.get("terms");
            if (terms.get("priceKind").asString().equals("UNKNOWN")) { unknown = true; assertTrue(terms.get("amount").isNull()); assertTrue(item.get("monthlyEquivalent").isNull()); }
            if (terms.get("priceKind").asString().equals("FREE")) { free = true; assertEquals("0", terms.get("amount").asString()); }
        }
        assertTrue(unknown && free);
        var regional = TREE.readTree(request("GET", "offers?currency=KZT").body());
        assertEquals(1, regional.get("total").asInt());
        assertEquals("1234.56", regional.get("items").get(0).get("terms").get("amount").asString());
        assertTrue(request("GET", "evidence?size=100").body().contains("STALE"));
    }
    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void deniesPublicMutation(String method) throws Exception {
        assertEquals(403, request(method, "products").statusCode());
        assertEquals(403, request(method, "publish").statusCode());
    }
    @Test void historyAndSingletonCannotBeDeletedOrRewritten() {
        UUID id = imported(); activate(id);
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM compass.catalog_active"));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compass.catalog_review SET reviewer = 'rewritten' WHERE version_id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.execute("TRUNCATE compass.catalog_publication CASCADE"));
        assertEquals(id, publication.current().versionId());
    }
    @Test void failedPointerUpdateRollsBackInsertedAuditEvent() {
        UUID old = imported(); activate(old);
        UUID next = imported(); review(next);
        var before = publication.current();
        long count = jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication", Long.class);
        jdbc.execute("CREATE FUNCTION compass.test_fail_activation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected activation failure'; END $$");
        jdbc.execute("CREATE TRIGGER test_fail_activation BEFORE UPDATE ON compass.catalog_active FOR EACH ROW EXECUTE FUNCTION compass.test_fail_activation()");
        try {
            assertThrows(DataAccessException.class, () -> publication.activate(next, before.revision(), false, "test", "Failure injection"));
            assertEquals(before, publication.current());
            assertEquals(count, jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication", Long.class));
        } finally {
            jdbc.execute("DROP TRIGGER test_fail_activation ON compass.catalog_active");
            jdbc.execute("DROP FUNCTION compass.test_fail_activation()");
        }
    }
    @Test void defaultPublicPolicyRejectsActivatedDemo() {
        activate(imported());
        var noDemo = new CatalogPolicy(new MockEnvironment(), jdbc);
        var publicReads = new CatalogReadService(jdbc, publication, noDemo, CLOCK);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, publicReads::meta);
    }
    @Test void readDuringSwitchNeverCombinesVersions() throws Exception {
        UUID a = imported(); activate(a);
        UUID b = imported(); review(b);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var writer = executor.submit(() -> publication.activate(b, publication.current().revision(), false, "test", "Read concurrency"));
            for (int i = 0; i < 8; i++) {
                var response = request("GET", "products/demo-studio");
                assertEquals(200, response.statusCode());
                var detail = TREE.readTree(response.body());
                assertTrue(List.of(a.toString(), b.toString()).contains(detail.get("versionId").asString()));
                assertEquals("demo-studio", detail.get("product").get("id").asString());
            }
            writer.get(20, TimeUnit.SECONDS);
        }
    }
}
