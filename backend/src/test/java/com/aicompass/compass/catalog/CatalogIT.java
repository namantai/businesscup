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
class CatalogIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(DockerImageName.parse(
            "postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withDatabaseName("compass_test")
            .withLabel("com.aicompass.scope", "p02-integration")
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
    @Autowired CatalogJson json;
    @Autowired CatalogImportValidator validator;
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;

    private UUID imported() { return importer.importManifest(mutated(d -> d.put("versionId", UUID.randomUUID().toString()))); }
    private int count(String table, UUID id) { return jdbc.queryForObject("SELECT count(*) FROM compass." + table + " WHERE version_id = ?", Integer.class, id); }

    @Test @Order(1)
    void migratesFreshDatabaseAndDoesNotAutoSeed() {
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM compass.catalog_version", Integer.class));
        assertEquals(26, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'compass'", Integer.class));
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }
    @Test void importsReproduciblyAndIdempotently() {
        String input = mutated(d -> d.put("versionId", UUID.randomUUID().toString()));
        UUID id = importer.importManifest(input);
        assertEquals(id, importer.importManifest(input));
        assertEquals(15, count("price_offer", id));
        assertEquals(json.read(input), importer.readPublishedArtifact(id));
        assertEquals(CLOCK.instant(), jdbc.queryForObject("SELECT published_at FROM compass.catalog_version WHERE id = ?", java.time.OffsetDateTime.class, id).toInstant());
    }
    @Test void concurrentRetryCreatesOneSnapshot() throws Exception {
        String input = mutated(d -> d.put("versionId", UUID.randomUUID().toString()));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> importer.importManifest(input));
            var second = executor.submit(() -> importer.importManifest(input));
            UUID id = first.get(20, TimeUnit.SECONDS);
            assertEquals(id, second.get(20, TimeUnit.SECONDS));
            assertEquals(15, count("price_offer", id));
        }
    }
    @Test void concurrentDifferentVersionsRemainIndependentlyReadable() throws Exception {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        String firstInput = mutated(d -> d.put("versionId", firstId.toString()));
        String secondInput = mutated(d -> {
            d.put("versionId", secondId.toString());
            row(d,"priceOffers",0).put("amount","13").put("upfrontAmount","13");
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var ready = new java.util.concurrent.CountDownLatch(2);
            var start = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start gate timeout");
                return importer.importManifest(firstInput);
            });
            var second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start gate timeout");
                return importer.importManifest(secondInput);
            });
            try { assertTrue(ready.await(5, TimeUnit.SECONDS)); }
            finally { start.countDown(); }
            assertEquals(firstId, first.get(20, TimeUnit.SECONDS));
            assertEquals(secondId, second.get(20, TimeUnit.SECONDS));
        }
        assertEquals(json.read(firstInput), importer.readPublishedArtifact(firstId));
        assertEquals(json.read(secondInput), importer.readPublishedArtifact(secondId));
        for (UUID id : List.of(firstId, secondId)) {
            assertEquals(15, count("price_offer", id));
            assertEquals("PUBLISHED", jdbc.queryForObject("SELECT state FROM compass.catalog_version WHERE id = ?", String.class, id));
        }
    }
    @Test void onePlanCanIncludeMultipleModelsAndAgents() {
        UUID id = importer.importManifest(mutated(d -> {
            d.put("versionId", UUID.randomUUID().toString());
            ((ArrayNode)d.get("agents")).add(row(d,"agents",0).deepCopy()
                    .put("id","demo-agent-second").put("name","Synthetic Second Agent"));
            var firstAccess = row(d,"planAgentAccess",0);
            firstAccess.put("planId","demo-plan-monthly");
            ((ArrayNode)d.get("planAgentAccess")).add(firstAccess.deepCopy()
                    .put("id","demo-included-second").put("agentId","demo-agent-second"));
        }));
        assertEquals(2, jdbc.queryForObject("SELECT count(DISTINCT model_id) FROM compass.plan_model_access WHERE version_id = ? AND plan_id = 'demo-plan-monthly'", Integer.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT count(DISTINCT agent_id) FROM compass.plan_agent_access WHERE version_id = ? AND plan_id = 'demo-plan-monthly'", Integer.class, id));
        assertEquals(15, count("price_offer", id));
    }
    @Test void preservesExactPricesFreeUnknownAndRegionalOffers() {
        UUID id = imported();
        assertEquals(new java.math.BigDecimal("0.000123"), jdbc.queryForObject("SELECT amount FROM compass.price_offer WHERE version_id = ? AND id = 'demo-offer-metered'", java.math.BigDecimal.class, id));
        assertEquals(new java.math.BigDecimal("120.01"), jdbc.queryForObject("SELECT upfront_amount FROM compass.price_offer WHERE version_id = ? AND id = 'demo-offer-annual'", java.math.BigDecimal.class, id));
        assertNull(jdbc.queryForObject("SELECT amount FROM compass.price_offer WHERE version_id = ? AND id = 'demo-offer-unknown'", java.math.BigDecimal.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT amount FROM compass.price_offer WHERE version_id = ? AND id = 'demo-offer-free'", java.math.BigDecimal.class, id).signum());
        assertEquals(2, jdbc.queryForObject("SELECT count(DISTINCT region_id) FROM compass.price_offer WHERE version_id = ? AND plan_id = 'demo-plan-monthly'", Integer.class, id));
    }
    @Test void mapsModelsAcrossProductsWithoutInheritingBenchmarks() {
        UUID id = imported();
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM compass.plan_model_access WHERE version_id = ? AND plan_id = 'demo-plan-monthly'", Integer.class, id));
        assertEquals(2, jdbc.queryForObject("""
            SELECT count(DISTINCT p.product_id) FROM compass.plan_model_access a
            JOIN compass.subscription_plan p ON (p.version_id, p.id) = (a.version_id, a.plan_id)
            WHERE a.version_id = ? AND a.model_id = 'demo-model-v1'
            """, Integer.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM compass.benchmark_observation WHERE version_id = ? AND model_id = 'demo-model-v2'", Integer.class, id));
    }
    @Test void preservesIncludedAgentSharedPoolNeedsStudentConditionAndEvidence() {
        UUID id = imported();
        assertEquals(1, count("plan_agent_access", id));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM compass.usage_limit WHERE version_id = ? AND pool_id = 'demo-pool-shared'", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(DISTINCT pool_id) FROM compass.usage_limit WHERE version_id = ?", Integer.class, id));
        assertEquals(3, count("need_capability", id));
        assertEquals("PROVIDER_VERIFICATION", jdbc.queryForObject("SELECT requirement FROM compass.eligibility_rule WHERE version_id = ?", String.class, id));
        assertEquals("STALE", jdbc.queryForObject("SELECT status FROM compass.evidence WHERE version_id = ? AND id = 'demo-e-stale-price'", String.class, id));
        assertEquals(CLOCK.instant(), jdbc.queryForObject("SELECT verified_at FROM compass.evidence WHERE version_id = ? AND id = 'demo-e-monthly-price'", java.time.OffsetDateTime.class, id).toInstant());
    }
    @ParameterizedTest @ValueSource(strings = {
        "UPDATE compass.price_offer SET amount = '0' WHERE id = 'demo-offer-unknown'",
        "UPDATE compass.price_offer SET amount = NULL WHERE id = 'demo-offer-free'",
        "UPDATE compass.price_offer SET upfront_amount = NULL WHERE id = 'demo-offer-annual'",
        "UPDATE compass.price_offer SET upfront_amount = 10 WHERE id = 'demo-offer-annual'",
        "UPDATE compass.price_offer SET amount = '-1' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET amount = '0.0000001' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET amount = 'NaN' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET currency = NULL WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET currency = 'ZZZ' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET currency = 'usd' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET plan_id = 'missing' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET price_evidence_id = 'demo-e-unknown-price' WHERE id = 'demo-offer-free'",
        "UPDATE compass.price_offer SET price_evidence_id = 'demo-e-monthly-tax' WHERE id = 'demo-offer-monthly'",
        "UPDATE compass.price_offer SET additional_tax = 0 WHERE id = 'demo-offer-tax-unknown'",
        "UPDATE compass.price_offer SET additional_fee = 0 WHERE id = 'demo-offer-fee-unknown'",
        "UPDATE compass.price_offer SET trial_days = NULL WHERE id = 'demo-offer-trial'",
        "UPDATE compass.subscription_plan SET exclusivity_group_id = 'demo-workbench-tiers' WHERE id = 'demo-plan-monthly'",
        "UPDATE compass.usage_limit SET pool_id = 'demo-pool-trial' WHERE id = 'demo-limit-image'",
        "UPDATE compass.quota_pool SET quantity = 0 WHERE id = 'demo-pool-free'",
        "UPDATE compass.capability_assertion SET state = 'SUPPORTED' WHERE id = 'demo-assertion-slides'",
        "UPDATE compass.capability_assertion SET state = 'UNSUPPORTED' WHERE id = 'demo-assertion-slides'",
        "UPDATE compass.product SET slug = 'demo-studio' WHERE id = 'demo-workbench'"
    })
    void databaseRejectsInvalidDataEvenWithoutApplicationValidator(String statement) {
        UUID draft = draftCopy(imported());
        assertThrows(DataAccessException.class, () -> jdbc.update(statement + " AND version_id = ?", draft));
    }
    @Test void databaseRejectsCrossVersionReferencesAndDuplicateIds() {
        UUID id = imported();
        UUID empty = emptyDraft();
        assertThrows(DataAccessException.class, () -> jdbc.update("""
            INSERT INTO compass.product SELECT ?, id, provider_id, slug, name, kind, state, metadata_evidence_id
            FROM compass.product WHERE version_id = ?
            """, empty, id));
        UUID draft = draftCopy(id);
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO compass.region SELECT * FROM compass.region WHERE version_id = ?", draft));
    }
    @Test void publishedRowsAndVersionCannotBeChangedDeletedInsertedMovedOrTruncated() {
        UUID id = imported();
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compass.price_offer SET amount = 1 WHERE version_id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM compass.price_offer WHERE version_id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO compass.region VALUES (?, 'demo-new', 'GB', 'Synthetic')", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compass.catalog_version SET reviewer = 'changed' WHERE id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM compass.catalog_version WHERE id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.execute("TRUNCATE compass.catalog_version CASCADE"));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE compass.region SET version_id = ? WHERE version_id = ?", emptyDraft(), draftCopy(id)));
    }
    @Test void failedLateImportRollsBackVersionAndPreviouslyInsertedRows() {
        UUID id = UUID.randomUUID();
        String input = mutated(d -> {
            d.put("versionId", id.toString());
            var duplicate = row(d,"priceOffers",0).deepCopy().put("id","demo-duplicate-regional-offer");
            ((ArrayNode)d.get("priceOffers")).add(duplicate);
        });
        // Shape/references are valid; DB unique regional offer key fails after earlier rows were inserted.
        validator.validate(json.read(input), true);
        assertThrows(DataAccessException.class, () -> importer.importManifest(input));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM compass.catalog_version WHERE id = ?", Integer.class, id));
        assertEquals(0, count("provider", id)); assertEquals(0, count("price_offer", id));
    }
    @Test void sameIdCannotBeReplacedAndNewVersionRetainsHistory() {
        UUID id = imported();
        String changed = mutated(d -> { d.put("versionId",id.toString()); row(d,"priceOffers",0).put("amount","13").put("upfrontAmount","13"); });
        assertThrows(IllegalArgumentException.class, () -> importer.importManifest(changed));
        assertEquals("12.34", importer.readPublishedArtifact(id).priceOffers().getFirst().amount());
        UUID next = UUID.randomUUID();
        var tree = (tools.jackson.databind.node.ObjectNode)TREE.readTree(changed); tree.put("versionId",next.toString());
        importer.importManifest(TREE.writeValueAsString(tree));
        assertEquals("13", importer.readPublishedArtifact(next).priceOffers().getFirst().amount());
        assertEquals("12.34", importer.readPublishedArtifact(id).priceOffers().getFirst().amount());
    }
    @Test void productionCannotImportReadOrStartWithDemo() {
        UUID id = imported();
        var env = new MockEnvironment(); env.setActiveProfiles("prod");
        var policy = new CatalogPolicy(env, jdbc);
        var prod = new CatalogImportService(jdbc,json,validator,policy,CLOCK);
        assertThrows(IllegalArgumentException.class, () -> prod.importManifest(fixture()));
        assertThrows(DataAccessException.class, () -> prod.readPublishedArtifact(id));
        assertThrows(IllegalStateException.class, () -> policy.run(null));
    }
    @Test void databaseRejectsDemoEvidenceInReviewedVersion() {
        UUID draft = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO compass.catalog_version (id,schema_version,dataset_kind,state,content_hash,as_of,reviewer,manifest_json)
            VALUES (?,1,'REVIEWED','DRAFT',?,?,'Synthetic negative test','{}')
            """, draft, hash(draft), CLOCK.instant().atOffset(java.time.ZoneOffset.UTC));
        assertThrows(DataAccessException.class, () -> jdbc.update("""
            INSERT INTO compass.evidence_source VALUES (?, 'demo-source', 'https://fixtures.example.invalid', 'Synthetic', 'SYNTHETIC', 'Test only')
            """, draft));
    }
    @Test void publicationAndConcurrentWriterSerializeOnTheVersionLock() throws Exception {
        UUID draft = draftCopy(imported());
        try (var executor = Executors.newSingleThreadExecutor(); var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("UPDATE compass.catalog_version SET state = 'PUBLISHED', published_at = ? WHERE id = ?")) {
                statement.setObject(1, CLOCK.instant().atOffset(java.time.ZoneOffset.UTC));
                statement.setObject(2, draft);
                statement.executeUpdate();
            }
            var started = new java.util.concurrent.CountDownLatch(1);
            var writer = executor.submit(() -> {
                started.countDown();
                return jdbc.update("INSERT INTO compass.region VALUES (?, 'demo-racing', 'GB', 'Synthetic race test')", draft);
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(java.util.concurrent.TimeoutException.class, () -> writer.get(200, TimeUnit.MILLISECONDS));
            connection.commit();
            var failure = assertThrows(java.util.concurrent.ExecutionException.class, () -> writer.get(5, TimeUnit.SECONDS));
            assertInstanceOf(DataAccessException.class, failure.getCause());
            assertEquals(2, count("region", draft));
        }
    }
    @Test void benchmarkCanStoreSignedMeasurementsWithoutTurningThemIntoMoney() {
        UUID id = importer.importManifest(mutated(d -> {
            d.put("versionId", UUID.randomUUID().toString());
            row(d,"benchmarkObservations",0).put("value","-0.125").put("metric","synthetic-signed-measurement");
        }));
        assertEquals(new java.math.BigDecimal("-0.125"), jdbc.queryForObject("SELECT value FROM compass.benchmark_observation WHERE version_id = ?", java.math.BigDecimal.class,id));
    }
    private UUID emptyDraft() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO compass.catalog_version (id,schema_version,dataset_kind,state,content_hash,as_of,reviewer,manifest_json)
            VALUES (?,1,'DEMO','DRAFT',?,?,'Synthetic constraint test','{}')
            """, id, hash(id), CLOCK.instant().atOffset(java.time.ZoneOffset.UTC));
        return id;
    }
    private static String hash(UUID id) { return id.toString().replace("-", "").repeat(2); }
    private UUID draftCopy(UUID source) {
        UUID target = emptyDraft();
        for (String table : List.of("provider","region","evidence_source","evidence","product","model_family","ai_model","agent",
                "capability","user_need","need_capability","exclusivity_group","subscription_plan","price_offer","plan_model_access",
                "plan_agent_access","capability_assertion","quota_pool","usage_limit","eligibility_rule","benchmark_observation","fx_snapshot")) {
            List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns WHERE table_schema = 'compass'
                AND table_name = ? AND column_name <> 'version_id' ORDER BY ordinal_position
                """, String.class, table);
            String names = String.join(",", columns);
            jdbc.update("INSERT INTO compass." + table + " (version_id," + names + ") SELECT ?," + names + " FROM compass." + table + " WHERE version_id = ?", target, source);
        }
        return target;
    }
}
