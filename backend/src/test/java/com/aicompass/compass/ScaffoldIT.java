package com.aicompass.compass;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScaffoldIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(DockerImageName.parse(
            "postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withDatabaseName("compass_test")
            .withLabel("com.aicompass.scope", "p01-integration")
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

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FilterChainProxy securityFilters;

    private HttpResponse<String> request(String method, String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", "application/problem+json, application/json")
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void appliesInitialMigrationToRealPostgres() {
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where success and version = '1'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.schemata where schema_name = 'compass'", Integer.class));
        assertTrue(jdbc.queryForObject("select version()", String.class).startsWith("PostgreSQL 17.12"));
    }

    @Test
    void exposesReadinessWithoutDatabaseDetails() throws Exception {
        var response = request("GET", "/actuator/health/readiness");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertFalse(response.body().contains("jdbc"));
        assertFalse(response.body().contains("components"));
    }

    @Test
    void returnsProblemDetailsForUnknownApi() throws Exception {
        var response = request("GET", "/api/v1/missing");
        assertEquals(404, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/problem+json"));
        assertTrue(response.body().contains("\"status\":404"));
        String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
        assertTrue(response.body().contains("\"requestId\":\"" + requestId + "\""));
        assertFalse(response.body().contains("stackTrace"));
    }

    @Test
    void rejectsMutationWithoutCsrf() throws Exception {
        var response = request("POST", "/api/v1/missing");
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/problem+json"));
        assertTrue(response.body().contains("\"instance\":\"/api/v1/missing\""));
        String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
        assertTrue(response.body().contains("\"requestId\":\"" + requestId + "\""));
    }

    @Test
    void retainsCsrfFilterInTheSecurityChain() {
        assertTrue(securityFilters.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .anyMatch(CsrfFilter.class::isInstance));
    }

    @Test
    void doesNotExposeEnvironmentEndpoint() throws Exception {
        assertEquals(403, request("GET", "/actuator/env").statusCode());
    }
    @Test
    void exportsPublicOpenApiWithExplicitUnknownAndNoMutationRoutes() throws Exception {
        var response = request("GET", "/api/v1/openapi");
        assertEquals(200, response.statusCode(), response.body());
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var schema = mapper.readTree(response.body());
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi.json"), response.body());

        assertEquals("3.0.1", schema.get("openapi").asString());
        var price = schema.at("/components/schemas/PriceOffer/properties/amount");
        assertTrue(price.get("nullable").asBoolean(), price.toString());
        assertEquals("string", price.get("type").asString());
        assertTrue(schema.at("/components/schemas/PageProduct/properties/versionId/nullable").asBoolean());
        assertTrue(schema.at("/components/schemas/Offer/properties/allInUpfront/nullable").asBoolean());
        assertTrue(schema.get("paths").has("/api/v1/catalog/products/{slug}"));
        for (var entry : schema.get("paths").properties()) {
            if (!entry.getKey().startsWith("/api/v1/catalog/")) continue;
            assertEquals(1, entry.getValue().size());
            assertTrue(entry.getValue().has("get"));
            assertTrue(entry.getValue().at("/get/responses/200/content/application~1json/schema/$ref").isString(), entry.toString());
        }
        assertTrue(schema.at("/paths/~1api~1v1~1recommendations~1preview/post").isObject());
        assertTrue(schema.at("/paths/~1api~1v1~1auth~1csrf/get").isObject());
        assertFalse(schema.at("/paths/~1api~1v1~1auth~1csrf/get").has("parameters"));
        var preview = schema.at("/paths/~1api~1v1~1recommendations~1preview/post");
        assertEquals("#/components/schemas/RecommendationProfile", preview.at("/requestBody/content/application~1json/schema/$ref").asString());
        for (String code : java.util.List.of("400", "403", "413", "429"))
            assertEquals("#/components/schemas/ApiProblem", preview.at("/responses/" + code + "/content/application~1problem+json/schema/$ref").asString());
        assertTrue(schema.at("/components/schemas/Costs/properties/recurringMonthly/nullable").asBoolean());
        assertEquals("string", schema.at("/components/schemas/Costs/properties/recurringMonthly/type").asString());
        assertFalse(response.body().contains("manifest_json"));
        assertFalse(response.body().contains("reviewer"));
    }
}
