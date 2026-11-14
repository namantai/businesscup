package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import com.github.dockerjava.api.model.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static com.aicompass.compass.recommendation.EngineFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"compass.catalog.allow-demo=true","server.servlet.session.cookie.secure=false"})
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecommendationIT {
    static final PostgreSQLContainer DATABASE=new PostgreSQLContainer(DockerImageName.parse("postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withDatabaseName("compass_test").withLabel("com.aicompass.scope","p04-integration")
            .withCreateContainerCmdModifier(c->{c.getHostConfig().withPortBindings(PortBinding.parse("127.0.0.1::5432"));});
    static {DATABASE.start();Runtime.getRuntime().addShutdownHook(new Thread(DATABASE::stop));}
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DATABASE::getJdbcUrl);r.add("spring.datasource.username",DATABASE::getUsername);r.add("spring.datasource.password",DATABASE::getPassword);}
    @AfterAll static void stop(){DATABASE.stop();}
    @TestConfiguration static class Time { @Bean @Primary Clock testClock(){return Clock.fixed(NOW.plusSeconds(14400),ZoneOffset.UTC);} }
    @Autowired CatalogImportService importer;
    @Autowired CatalogPublicationService publication;
    @Autowired CatalogReadService catalog;
    @Autowired RecommendationService service;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    static UUID first, second;
    private HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    private HttpResponse<String> request(HttpClient client,String method,String path,String body,String csrf) throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path));
        if(csrf!=null)r.header("X-CSRF-TOKEN",csrf);
        if(method.equals("POST"))r.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));else r.GET();
        return client.send(r.build(),HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(HttpClient c)throws Exception {var r=request(c,"GET","/auth/csrf","",null);assertEquals(200,r.statusCode(),r.body());assertTrue(r.headers().firstValue("Cache-Control").orElse("").contains("no-store"));return JSON.readTree(r.body()).get("token").asString();}
    private HttpResponse<String> preview(RecommendationProfile p)throws Exception {var c=client();return request(c,"POST","/recommendations/preview",JSON.writeValueAsString(p),csrf(c));}
    private void publish(UUID id){String hash=jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?",String.class,id);publication.review(id,hash,"p04-test");publication.activate(id,publication.current().revision(),false,"p04-test","Synthetic test");}
    @Test @Order(1) void emptyCatalogReturnsHonestStateAndCsrfIsRequired() throws Exception {
        assertEquals(403,request(client(),"POST","/recommendations/preview",JSON.writeValueAsString(student()),null).statusCode());
        var r=preview(student());assertEquals(200,r.statusCode(),r.body());assertEquals("NO_PUBLISHED_CATALOG",JSON.readTree(r.body()).get("status").asString());
    }
    @Test @Order(2) void importRemainsPrivateUntilPublishThenPreviewUsesNormalizedSnapshot() throws Exception {
        first=importer.importManifest(manifest());assertNull(catalog.currentSnapshot());publish(first);
        var snap=catalog.currentSnapshot();assertEquals(first,snap.version());assertEquals(9,snap.offers().size());
        var r=preview(student());assertEquals(200,r.statusCode(),r.body());var json=JSON.readTree(r.body());
        assertEquals(first.toString(),json.get("catalogVersion").asString());assertEquals("MATCH",json.get("status").asString());assertTrue(json.get("requestId").isString());
        assertEquals("0",json.at("/bundles/0/costs/recurringMonthly").asString());
        Files.writeString(Path.of("target/p04-student-response.json"),r.body());
    }
    @Test @Order(3) void editorAndStudentRealHttpResponsesDifferWithoutPersistingProfiles() throws Exception {
        long before=jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication",Long.class);
        var r=preview(editor());assertEquals(200,r.statusCode(),r.body());var json=JSON.readTree(r.body());
        assertEquals("creator",json.at("/bundles/0/items/0/productId").asString());assertEquals("15",json.at("/bundles/0/costs/recurringMonthly").asString());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM compass.catalog_publication",Long.class));
        Files.writeString(Path.of("target/p04-editor-response.json"),r.body());
    }
    @Test @Order(4) void invalidProfileAndLargeBodiesAreProblemDetails() throws Exception {
        var c=client();var token=csrf(c);var r=request(c,"POST","/recommendations/preview","{\"premium\":true}",token);
        assertEquals(400,r.statusCode(),r.body());assertTrue(r.headers().firstValue("content-type").orElse("").contains("problem+json"));assertTrue(JSON.readTree(r.body()).has("requestId"));
        r=request(c,"POST","/recommendations/preview"," ".repeat(65537),token);assertEquals(413,r.statusCode());
    }
    @Test @Order(5) void newVersionRollbackAndConcurrentReadsNeverMixSnapshots() throws Exception {
        var tree=(tools.jackson.databind.node.ObjectNode)JSON.readTree(manifest());second=UUID.randomUUID();tree.put("versionId",second.toString());row(tree,"priceOffers","creator-offer").put("amount","18").put("upfrontAmount","18");
        importer.importManifest(JSON.writeValueAsString(tree));publish(second);
        assertEquals("18",service.preview(JSON.writeValueAsString(editor())).bundles().getFirst().costs().recurringMonthly());
        try(var workers=Executors.newFixedThreadPool(2)) {
            var writer=workers.submit(()->{for(int i=0;i<6;i++)publication.activate(i%2==0?first:second,publication.current().revision(),true,"p04-test","Concurrent switch");});
            var reader=workers.submit(()->{for(int i=0;i<16;i++){var s=catalog.currentSnapshot();var price=s.offers().stream().filter(o->o.id().equals("creator-offer")).findFirst().orElseThrow().amount();assertEquals(s.version().equals(first)?"15":"18",price);}});
            writer.get(30,TimeUnit.SECONDS);reader.get(30,TimeUnit.SECONDS);
        }
        publication.activate(first,publication.current().revision(),true,"p04-test","Rollback");
        assertEquals("15",service.preview(JSON.writeValueAsString(editor())).bundles().getFirst().costs().recurringMonthly());
    }
    @Test @Order(6) void realLimitedCatalogDoesNotInventVideoOrKzCheckoutPrices() throws Exception {
        try(var in=getClass().getResourceAsStream("/data/catalog/catalog-2026-11-14-r2.json")) {
            var id=importer.importManifest(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8));publish(id);
            var p=change(editor(),t->t.put("asOf",NOW.plusSeconds(14400).toString()));var r=preview(p);assertEquals(200,r.statusCode(),r.body());
            var json=JSON.readTree(r.body());assertEquals(0,json.get("bundles").size());assertEquals(3,json.at("/search/catalogProducts").asInt());
            assertFalse(json.get("unmetRequirements").isEmpty());assertFalse(r.body().contains("reviewer"));assertFalse(r.body().contains("manifest_json"));
            Files.writeString(Path.of("target/p04-real-catalog-response.json"),r.body());
        }
    }
    @Test @Order(7) void publicImportAndSavedRecommendationRoutesRemainForbidden() throws Exception {
        var c=client();var token=csrf(c);
        for(String path:List.of("/catalog/import","/catalog/publish","/recommendations","/profiles"))assertEquals(403,request(c,"POST",path,"{}",token).statusCode());
    }
    @Test @Order(8) void publicPreviewIsRateLimited() throws Exception {
        boolean limited=false;
        for(int i=0;i<32;i++){var r=request(client(),"GET","/auth/csrf","",null);if(r.statusCode()==429){assertTrue(r.headers().firstValue("Retry-After").isPresent());assertTrue(JSON.readTree(r.body()).has("requestId"));limited=true;break;}}
        assertTrue(limited);
    }
}
