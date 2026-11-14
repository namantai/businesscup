package com.aicompass.compass.identity;

import com.aicompass.compass.CompassApplication;
import com.aicompass.compass.catalog.*;
import com.aicompass.compass.recommendation.*;
import com.github.dockerjava.api.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityIT {
    @TempDir static Path mail;
    PostgreSQLContainer db;
    ConfigurableApplicationContext context;
    JdbcTemplate jdbc;
    String base;
    final JsonMapper json=JsonMapper.builder().build();
    final String password="Synthetic-only-password-234!";
    @BeforeAll void start() {
        db=new PostgreSQLContainer(DockerImageName.parse("postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"))
            .withLabel("com.aicompass.scope","p06-integration")
            .withCreateContainerCmdModifier(c->c.withHostConfig(c.getHostConfig().withPortBindings(PortBinding.parse("127.0.0.1::5432"))));
        db.start();Runtime.getRuntime().addShutdownHook(new Thread(db::stop));
        try (var connection = java.sql.DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT count(*) FROM information_schema.schemata WHERE schema_name='compass'")) {
            rows.next(); assertEquals(0, rows.getInt(1), "Test PostgreSQL must start empty");
        } catch (java.sql.SQLException error) { throw new AssertionError("Cannot inspect isolated PostgreSQL", error); }
    }
    void startApp(){
        context=new SpringApplicationBuilder(CompassApplication.class).profiles("test").run(
            "--server.port=0","--spring.datasource.url="+db.getJdbcUrl(),"--spring.datasource.username="+db.getUsername(),"--spring.datasource.password="+db.getPassword(),
            "--server.servlet.session.cookie.secure=false","--compass.catalog.allow-demo=true","--compass.identity.mail-directory="+mail,
            "--compass.monitoring.enabled=false","--logging.level.root=ERROR");
        base="http://127.0.0.1:"+((ServletWebServerApplicationContext)context).getWebServer().getPort()+"/api/v1";
        jdbc=context.getBean(JdbcTemplate.class);
    }
    @AfterAll void stop(){if(context!=null)context.close();if(db!=null)db.stop();}
    // Each independent scenario gets a real application lifecycle and its own in-memory preview rate window.
    // Rate-limit tests keep the same application within the scenario; production limits are unchanged.
    @BeforeEach void rateWindow(){if(context!=null)context.close();startApp();jdbc.update("DELETE FROM compass.auth_rate");jdbc.update("UPDATE compass.watched_stack SET active=false");jdbc.update("UPDATE compass.user_account SET role='USER'");}
    class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        String token, header;
        HttpResponse<String> call(String method,String path,Object body,boolean csrf)throws Exception {
            var b=HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/json");
            if(csrf&&token!=null)b.header(header,token);
            return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        void csrf()throws Exception{var r=call("GET","/auth/csrf",null,false);assertEquals(200,r.statusCode());var data=json.readTree(r.body());token=data.get("token").asString();header=data.get("headerName").asString();}
        String session(){return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("SESSION")).findFirst().orElseThrow().getValue();}
    }
    String email(){return UUID.randomUUID()+"@example.invalid";}
    Map<String,Object> credentials(String email){return Map.of("email",email,"password",password);}
    String token(String email,String purpose)throws Exception {
        try(var files=Files.list(mail)){
            for(var p:files.toList()){
                String s=Files.readString(p);
                if(s.startsWith("To: "+email+"\nPurpose: "+purpose+"\n"))return s.substring(s.indexOf("#token=")+7).strip();
            }
        }
        throw new AssertionError("No local message");
    }
    Browser verified(String email)throws Exception{
        Browser b=new Browser();b.csrf();assertEquals(202,b.call("POST","/auth/register",credentials(email),true).statusCode());
        assertEquals(200,b.call("POST","/auth/verify",Map.of("token",token(email,"VERIFY")),true).statusCode());return b;
    }
    Browser signedIn(String email)throws Exception{var b=verified(email);assertEquals(200,b.call("POST","/auth/login",credentials(email),true).statusCode());b.csrf();return b;}
    UUID id(String email){return jdbc.queryForObject("SELECT id FROM compass.user_account WHERE email=?",UUID.class,email);}
    void status(int expected,HttpResponse<String> response){assertEquals(expected,response.statusCode(),()->"HTTP "+response.statusCode()+" (response body withheld)");}
    @Test void flywayCreatesStandardSessionTablesAndNoAutomaticInitializer(){
        assertEquals("never",context.getEnvironment().getProperty("spring.session.jdbc.initialize-schema"));
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"), jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class));
        context.getBean(org.flywaydb.core.Flyway.class).validate();
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version IN ('5','6') AND success",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema='compass' AND table_name IN ('spring_session','spring_session_attributes')",Integer.class));
    }
    @Test void registrationNormalizationDuplicateEnumerationAndVerification()throws Exception{
        String email=email();var b=new Browser();b.csrf();var first=b.call("POST","/auth/register",credentials(email.toUpperCase(Locale.ROOT)),true);
        var duplicate=b.call("POST","/auth/register",credentials(email),true);status(202,first);assertEquals(first.body(),duplicate.body());
        status(401,b.call("POST","/auth/login",credentials(email),true));
        String t=token(email,"VERIFY");status(200,b.call("POST","/auth/verify",Map.of("token",t),true));
        status(400,b.call("POST","/auth/verify",Map.of("token",t),true));
        status(200,b.call("POST","/auth/login",credentials(email),true));
        String hash=jdbc.queryForObject("SELECT password_hash FROM compass.user_account WHERE email=?",String.class,email);
        assertFalse(password.equals(hash),"Password stored without encoding");assertFalse(duplicate.body().contains(hash));
    }
    @Test void loginFixationCsrfRotationAndLogoutInvalidation()throws Exception{
        String email=email();var b=verified(email);String old=b.session(),oldCsrf=b.token;
        status(200,b.call("POST","/auth/login",credentials(email),true));assertFalse(old.equals(b.session()),"Session ID must rotate on login");
        var fixed=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/me")).header("Cookie","SESSION="+old).GET().build(),HttpResponse.BodyHandlers.ofString());status(401,fixed);
        status(403,b.call("POST","/auth/logout",null,true)); // old pre-login token
        b.csrf();status(200,b.call("GET","/me",null,false));String signed=b.session();String loginCsrf=b.token;
        status(204,b.call("POST","/auth/logout",null,true));status(401,b.call("GET","/me",null,false));
        b.csrf();assertFalse(loginCsrf.equals(b.token),"CSRF must rotate on logout");status(403,b.call("POST","/auth/login",credentials(email),false));
        String fresh=b.token;b.token=loginCsrf;
        status(403,b.call("POST","/auth/login",credentials(email),true));
        b.token=fresh;status(200,b.call("POST","/auth/login",credentials(email),true));
        var replay=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/me")).header("Cookie","SESSION="+signed).GET().build(),HttpResponse.BodyHandlers.ofString());status(401,replay);
    }
    @Test void authorizedSessionSurvivesRealBackendRestart()throws Exception{
        String email=email();var b=signedIn(email);String saved=b.session();
        status(201,b.call("POST","/profiles",Map.of("name","Survives restart","profile",profile(),"revision",0),true));
        context.close();startApp();
        assertEquals(1,json.readTree(b.call("GET","/profiles",null,false).body()).size());
        status(200,b.call("GET","/me",null,false));assertTrue(saved.equals(b.session()),"Session changed after application restart");
        assertEquals(id(email).toString(),json.readTree(b.call("GET","/me",null,false).body()).get("id").asString());
    }
    @Test void expirationRejectsStoredSessionAndOldCsrf()throws Exception{
        String email=email();var b=signedIn(email);
        jdbc.update("UPDATE compass.spring_session SET last_access_time=0,expiry_time=0 WHERE principal_name=?",id(email).toString());
        status(401,b.call("GET","/me",null,false));status(403,b.call("POST","/auth/login",credentials(email),true));b.csrf();status(200,b.call("POST","/auth/login",credentials(email),true));
    }
    @Test void concurrentSessionsAndPasswordResetRevokeBoth()throws Exception{
        String email=email();var a=signedIn(email);var b=new Browser();b.csrf();status(200,b.call("POST","/auth/login",credentials(email),true));b.csrf();
        status(200,a.call("GET","/me",null,false));status(200,b.call("GET","/me",null,false));
        status(202,a.call("POST","/auth/password/forgot",Map.of("email",email),true));String t=token(email,"RESET");
        status(200,a.call("POST","/auth/password/reset",Map.of("token",t,"password",password+"new"),true));
        status(401,a.call("GET","/me",null,false));status(401,b.call("GET","/me",null,false));
        a.csrf();status(400,a.call("POST","/auth/password/reset",Map.of("token",t,"password",password),true));
        status(401,a.call("POST","/auth/login",credentials(email),true));status(200,a.call("POST","/auth/login",Map.of("email",email,"password",password+"new"),true));
    }
    @Test void expiredAndInvalidTokensNeverVerify()throws Exception{
        String email=email();var b=new Browser();b.csrf();status(202,b.call("POST","/auth/register",credentials(email),true));String t=token(email,"VERIFY");
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.identity_token WHERE token_hash=?",Integer.class,t));
        jdbc.update("UPDATE compass.identity_token SET expires_at=now()-interval '1 hour' WHERE user_id=?",id(email));
        status(400,b.call("POST","/auth/verify",Map.of("token",t),true));status(400,b.call("POST","/auth/verify",Map.of("token","X".repeat(43)),true));
    }
    @Test void resendReplacesTokenAndResetIsEnumerationSafe()throws Exception{
        String email=email();var b=new Browser();b.csrf();status(202,b.call("POST","/auth/register",credentials(email),true));String old=token(email,"VERIFY");
        status(202,b.call("POST","/auth/verification/resend",Map.of("email",email),true));status(400,b.call("POST","/auth/verify",Map.of("token",old),true));
        var known=b.call("POST","/auth/password/forgot",Map.of("email",email),true);var unknown=b.call("POST","/auth/password/forgot",Map.of("email",email()),true);
        assertEquals(known.statusCode(),unknown.statusCode());assertEquals(known.body(),unknown.body());
    }
    @Test void csrfMissingInvalidAndMassAssignment()throws Exception{
        var b=new Browser();status(403,b.call("POST","/auth/register",credentials(email()),false));b.csrf();b.token="invalid";
        status(403,b.call("POST","/auth/register",credentials(email()),true));b.csrf();
        var input=new HashMap<>(credentials(email()));input.put("role","ADMIN");status(400,b.call("POST","/auth/register",input,true));
        input.remove("role");input.put("premium",true);status(400,b.call("POST","/auth/register",input,true));
    }
    @Test void rateLimitsPersistAndUnknownLoginMessageMatchesWrongPassword()throws Exception{
        String email=email();var b=verified(email);
        var known=b.call("POST","/auth/login",Map.of("email",email,"password",password+"bad"),true);
        var unknown=b.call("POST","/auth/login",credentials(email()),true);
        status(401,known);status(401,unknown);assertEquals(json.readTree(known.body()).get("detail"),json.readTree(unknown.body()).get("detail"));
        for(int i=0;i<9;i++)status(401,b.call("POST","/auth/login",Map.of("email",email,"password",password+"bad"),true));
        status(429,b.call("POST","/auth/login",credentials(email),true));
    }
    Object profile()throws Exception {
        var node=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/profiles/student.json")));
        node.putNull("asOf");return node;
    }
    @Test void freeProfileOwnershipRevisionAndMassAssignment()throws Exception{
        String email=email();var a=signedIn(email);var b=signedIn(email());
        var body=Map.of("name","My needs","profile",profile(),"revision",0);
        var created=a.call("POST","/profiles",body,true);status(201,created);String path="/profiles/"+json.readTree(created.body()).get("id").asString();
        status(409,a.call("POST","/profiles",body,true));status(404,b.call("GET",path,null,false));status(404,b.call("PATCH",path,body,true));status(404,b.call("DELETE",path,null,true));
        status(409,a.call("PATCH",path,body,true));
        status(200,a.call("PATCH",path,Map.of("name","Revised","profile",profile(),"revision",1),true));
        var injected=new HashMap<String,Object>(body);injected.put("ownerId",id(email).toString());status(400,b.call("POST","/profiles",injected,true));
        status(403,a.call("GET","/admin/catalog",null,false));
        status(204,a.call("DELETE",path,null,true));
    }
    @Test void snapshotsAreServerDerivedHistoricalOwnedAndDeletedWithAccount()throws Exception{
        var importer=context.getBean(CatalogImportService.class);var publication=context.getBean(CatalogPublicationService.class);
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=UUID.randomUUID();doc.put("versionId",version.toString());importer.importManifest(doc.toString());
        String hash=jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?",String.class,version);
        publication.review(version,hash,"p06-it");publication.activate(version,publication.current().revision(),false,"p06-it","Synthetic test");
        String email=email();var a=signedIn(email);var b=signedIn(email());Object p=profile();
        status(201,a.call("POST","/profiles",Map.of("name","My profile","profile",p,"revision",0),true));
        var preview=a.call("POST","/recommendations/preview",p,true);status(200,preview);var r=json.readTree(preview.body());
        var input=Map.of("profile",p,"catalogVersion",version.toString(),"asOf",r.get("asOf").asString(),"engineVersion",r.get("engineVersion").asString(),"policyVersion",r.get("policyVersion").asString());
        var saved=a.call("POST","/recommendations",input,true);status(201,saved);var snap=json.readTree(saved.body());String snapshot=snap.get("id").asString();
        status(404,b.call("GET","/recommendations/"+snapshot,null,false));status(404,b.call("DELETE","/recommendations/"+snapshot,null,true));
        String bundle=snap.at("/result/bundles/0/id").asString();
        status(404,b.call("POST","/saved-stacks",Map.of("snapshotId",snapshot,"bundleId",bundle),true));
        status(201,a.call("POST","/saved-stacks",Map.of("snapshotId",snapshot,"bundleId",bundle),true));
        var export=a.call("GET","/me/export",null,false);status(200,export);
        assertFalse(export.body().contains("password_hash"));assertFalse(export.body().contains("token"));assertFalse(export.body().contains("SESSION"));
        assertEquals(1,json.readTree(export.body()).get("profiles").size());assertEquals(1,json.readTree(export.body()).get("recommendations").size());
        doc.put("versionId",UUID.randomUUID().toString());UUID newer=importer.importManifest(doc.toString());hash=jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?",String.class,newer);
        publication.review(newer,hash,"p06-it");publication.activate(newer,publication.current().revision(),false,"p06-it","New snapshot");
        assertTrue(snap.equals(json.readTree(a.call("GET","/recommendations/"+snapshot,null,false).body())),"Stored snapshot changed");
        UUID owner=id(email);status(204,a.call("DELETE","/me",Map.of("password",password,"confirm",true),true));status(401,a.call("GET","/me",null,false));
        for(String table:List.of("saved_profile","recommendation_snapshot","saved_stack"))assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass."+table+" WHERE owner_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.spring_session WHERE principal_name=?",Integer.class,owner.toString()));
        status(200,b.call("GET","/catalog/meta",null,false));
        var guest=new Browser();guest.csrf();status(200,guest.call("POST","/recommendations/preview",p,true));
    }

    @Test void expiredResetDoesNotChangePassword() throws Exception {
        String email=email();var b=signedIn(email);
        status(202,b.call("POST","/auth/password/forgot",Map.of("email",email),true));String t=token(email,"RESET");
        jdbc.update("UPDATE compass.identity_token SET expires_at=now()-interval '1 hour' WHERE user_id=? AND purpose='RESET'",id(email));
        status(400,b.call("POST","/auth/password/reset",Map.of("token",t,"password",password+"new"),true));
        status(200,b.call("GET","/me",null,false));
    }
    @Test void parallelFreeProfileCreationCannotBypassLimit() throws Exception {
        var b=signedIn(email());var body=Map.of("name","Concurrent","profile",profile(),"revision",0);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->b.call("POST","/profiles",body,true).statusCode());
            var c=pool.submit(()->b.call("POST","/profiles",body,true).statusCode());
            assertEquals(Set.of(201,409),Set.of(a.get(),c.get()));
        }
        assertEquals(1,json.readTree(b.call("GET","/profiles",null,false).body()).size());
    }
    @Test void staleAccountPreconditionIsRejected() throws Exception {
        var b=signedIn(email());
        var request=HttpRequest.newBuilder(URI.create(base+"/me")).header("X-Compass-Account",UUID.randomUUID().toString()).GET().build();
        status(409,b.client.send(request,HttpResponse.BodyHandlers.ofString()));
        status(200,b.call("GET","/me",null,false));
    }
    @Test void secretsStayOutOfLogsResponsesAndPrivateMailboxPermissions(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        String email=email();var b=verified(email);String t=token(email,"VERIFY");
        status(200,b.call("POST","/auth/login",credentials(email),true));b.csrf();
        var response=b.call("GET","/me/export",null,false);String sessionId=jdbc.queryForObject("SELECT session_id FROM compass.spring_session WHERE principal_name=?",String.class,id(email).toString());
        String hash=jdbc.queryForObject("SELECT password_hash FROM compass.user_account WHERE email=?",String.class,email);
        for(String secret:List.of(password,t,sessionId,hash,b.token)) {assertFalse(output.getAll().contains(secret),"Secret in application output");assertFalse(response.body().contains(secret),"Secret in export");}
        try(var files=Files.list(mail)){for(var file:files.toList())assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),Files.getPosixFilePermissions(file));}
        var csrfResponse=b.call("GET","/auth/csrf",null,false); // Existing cookie may not be reissued.
        var guest=new Browser();var cookie=guest.call("GET","/auth/csrf",null,false).headers().allValues("Set-Cookie").toString();
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.toLowerCase(Locale.ROOT).contains("samesite=lax"));
    }
    @Test void deleteNeedsPasswordAndConfirmationAndRemovesLocalMessages() throws Exception {
        String email=email();var b=signedIn(email);
        status(400,b.call("DELETE","/me",Map.of("password",password,"confirm",false),true));
        status(403,b.call("DELETE","/me",Map.of("password",password+"bad","confirm",true),true));
        status(200,b.call("GET","/me",null,false));
        status(204,b.call("DELETE","/me",Map.of("password",password,"confirm",true),true));
        try(var files=Files.list(mail)){for(var file:files.toList())assertFalse(Files.readString(file).startsWith("To: "+email+"\n"));}
    }
    @Test void bootstrapRequiresVerifiedAccountAndIsOneTime() throws Exception {
        var tx=new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
        String email=email();var b=signedIn(email);UUID owner=id(email);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->new AdminBootstrap(jdbc).run(new org.springframework.boot.DefaultApplicationArguments("--identity-admin="+UUID.randomUUID()))));
        tx.executeWithoutResult(s->new AdminBootstrap(jdbc).run(new org.springframework.boot.DefaultApplicationArguments("--identity-admin="+owner)));
        assertEquals("ADMIN",jdbc.queryForObject("SELECT role FROM compass.user_account WHERE id=?",String.class,owner));
        status(401,b.call("GET","/me",null,false));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->new AdminBootstrap(jdbc).run(new org.springframework.boot.DefaultApplicationArguments("--identity-admin="+owner))));
    }
    @Test void ipLimitIncludesRetryAfterBeforeResponseIsCommitted() throws Exception {
        var b = signedIn(email());
        jdbc.update("UPDATE compass.auth_rate SET attempts=300 WHERE bucket_hash=?", AuthRateLimiter.hash("ip:127.0.0.1"));
        var response = b.call("GET", "/me", null, false);
        status(429, response);
        assertEquals("900", response.headers().firstValue("Retry-After").orElse("missing"));
    }
    @Test void personalSnapshotDetailCannotBypassIpLimit() throws Exception {
        var b = signedIn(email());
        jdbc.update("UPDATE compass.auth_rate SET attempts=300 WHERE bucket_hash=?", AuthRateLimiter.hash("ip:127.0.0.1"));
        status(429, b.call("GET", "/recommendations/" + UUID.randomUUID(), null, false));
    }
    @Test void absoluteSessionLifetimeAppliesEvenWithRecentActivity() throws Exception {
        String email = email(); var b = signedIn(email);
        String sessionId = jdbc.queryForObject("SELECT session_id FROM compass.spring_session WHERE principal_name=?", String.class, id(email).toString());
        // JdbcSession is package-private; use the public SessionRepository boundary.
        @SuppressWarnings("unchecked")
        var repository = (org.springframework.session.SessionRepository<org.springframework.session.Session>)
                (org.springframework.session.SessionRepository<?>) context.getBean(org.springframework.session.jdbc.JdbcIndexedSessionRepository.class);
        var stored = repository.findById(sessionId);
        assertNotNull(stored);
        assertEquals(Duration.ofMinutes(30), stored.getMaxInactiveInterval());
        stored.setAttribute("authenticatedAt", System.currentTimeMillis() - Duration.ofHours(13).toMillis());
        repository.save(stored);
        status(401, b.call("GET", "/me", null, false));
        status(403, b.call("POST", "/auth/login", credentials(email), true));
    }
    @Test void concurrentResetConsumesTokenOnlyOnceAndRevokesSession() throws Exception {
        String email = email(); var owner = signedIn(email);
        status(202, owner.call("POST", "/auth/password/forgot", Map.of("email", email), true));
        var input = Map.of("token", token(email, "RESET"), "password", password + "new");
        var first = new Browser(); first.csrf(); var second = new Browser(); second.csrf();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { start.await(); return first.call("POST", "/auth/password/reset", input, true).statusCode(); });
            var b = pool.submit(() -> { start.await(); return second.call("POST", "/auth/password/reset", input, true).statusCode(); });
            start.countDown();
            assertEquals(List.of(200, 400), java.util.stream.Stream.of(a.get(), b.get()).sorted().toList());
        }
        status(401, owner.call("GET", "/me", null, false));
        status(200, first.call("POST", "/auth/login", Map.of("email", email, "password", password + "new"), true));
    }
    @Test void concurrentRateLimitDoesNotLoseAttempts() throws Exception {
        var limiter = context.getBean(AuthRateLimiter.class);
        String key = "concurrent-test:" + UUID.randomUUID();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(12)) {
            var tasks = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 12; i++) tasks.add(pool.submit(() -> {
                start.await();
                try { limiter.take(key, 5); return 200; }
                catch (org.springframework.web.server.ResponseStatusException error) { return error.getStatusCode().value(); }
            }));
            start.countDown();
            var codes = new ArrayList<Integer>(); for (var task : tasks) codes.add(task.get());
            assertEquals(5, codes.stream().filter(code -> code == 200).count());
            assertEquals(7, codes.stream().filter(code -> code == 429).count());
            assertEquals(12, jdbc.queryForObject("SELECT attempts FROM compass.auth_rate WHERE bucket_hash=?", Integer.class, AuthRateLimiter.hash(key)));
        }
    }

    Object ledgerBody(long revision) {
        return json.readTree("""
            {"revision":%d,"terms":{"name":"Synthetic ledger","status":"ACTIVE","catalogVersion":null,"planId":null,"offerId":null,
            "currency":"USD","amount":"12","priceOrigin":"USER_REPORTED","billingPeriod":"MONTH","renewalAnchor":"2026-11-14",
            "nextRenewal":"2026-11-14","billingZone":"UTC","paidFrom":null,"paidUntil":null,"paidAmount":null,
            "autoRenew":"YES","trialUntil":null,"obligation":"NONE","commitmentUntil":null,"cancellationAvailableFrom":"2026-11-14",
            "additionalPerPeriod":"0","transitionCost":"0","mustKeep":false}}
            """.formatted(revision));
    }
    @Test void subscriptionOwnershipCsrfRevisionsExportAndCascade()throws Exception {
        String email=email();var a=signedIn(email);var b=signedIn(email());var guest=new Browser();
        status(401,guest.call("GET","/subscriptions",null,false));
        status(403,a.call("POST","/subscriptions",ledgerBody(0),false));
        var created=a.call("POST","/subscriptions",ledgerBody(0),true);status(201,created);
        String key=json.readTree(created.body()).get("id").asString(),path="/subscriptions/"+key;
        status(404,b.call("GET",path,null,false));status(404,b.call("PATCH",path,ledgerBody(1),true));status(404,b.call("DELETE",path+"?revision=1",null,true));
        status(409,a.call("POST","/subscriptions",ledgerBody(0),true));
        status(200,a.call("PATCH",path,ledgerBody(1),true));status(409,a.call("PATCH",path,ledgerBody(1),true));
        status(409,a.call("DELETE",path+"?revision=1",null,true));
        var export=json.readTree(a.call("GET","/me/export",null,false).body());assertEquals(1,export.get("subscriptions").size());
        assertEquals("USER_REPORTED",export.at("/subscriptions/0/terms/priceOrigin").asString());
        status(204,a.call("DELETE",path+"?revision=2",null,true));
        status(201,a.call("POST","/subscriptions",ledgerBody(0),true));
        UUID owner=id(email);status(204,a.call("DELETE","/me",Map.of("password",password,"confirm",true),true));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.user_subscription WHERE owner_id=?",Integer.class,owner));
        status(401,a.call("GET","/subscriptions",null,false));
    }
    @Test void subscriptionConcurrentUpdateOneWinnerAndDatabaseConstraints()throws Exception {
        String email=email();var a=signedIn(email);var created=a.call("POST","/subscriptions",ledgerBody(0),true);status(201,created);
        UUID key=UUID.fromString(json.readTree(created.body()).get("id").asString());String path="/subscriptions/"+key;
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var one=pool.submit(()->a.call("PATCH",path,ledgerBody(1),true).statusCode());
            var two=pool.submit(()->a.call("PATCH",path,ledgerBody(1),true).statusCode());
            assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get(),two.get()).sorted().toList());
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE compass.user_subscription SET amount=-1 WHERE id=?",key));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE compass.user_subscription SET price_state='UNKNOWN' WHERE id=?",key));
        var numeric=(tools.jackson.databind.node.ObjectNode)ledgerBody(0);((tools.jackson.databind.node.ObjectNode)numeric.get("terms")).put("amount",12);
        status(400,a.call("POST","/subscriptions",numeric,true));
        var injected=(tools.jackson.databind.node.ObjectNode)ledgerBody(0);injected.put("ownerId",id(email).toString());status(400,a.call("POST","/subscriptions",injected,true));
    }
    @Test void subscriptionComparisonPinsLedgerCatalogAndRateLimit()throws Exception {
        var importer=context.getBean(CatalogImportService.class);var publication=context.getBean(CatalogPublicationService.class);
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=UUID.randomUUID();doc.put("versionId",version.toString());importer.importManifest(doc.toString());
        publication.review(version,jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?",String.class,version),"p07-it");
        publication.activate(version,publication.current().revision(),false,"p07-it","Isolated ledger comparison");
        var a=signedIn(email());var created=a.call("POST","/subscriptions",ledgerBody(0),true);status(201,created);String key=json.readTree(created.body()).get("id").asString();
        var p=(tools.jackson.databind.node.ObjectNode)profile();p.put("portfolioMode","REVIEW");
        var input=Map.of("profile",p,"catalogVersion",version,"revisions",Map.of(key,1));
        var compared=a.call("POST","/subscriptions/analyze",input,true);status(200,compared);
        assertEquals(version.toString(),json.readTree(compared.body()).get("catalogVersion").asString());
        assertEquals("144",json.readTree(compared.body()).at("/current/costs/horizonTotal").asString());
        status(409,a.call("POST","/subscriptions/analyze",Map.of("profile",p,"catalogVersion",version,"revisions",Map.of()),true));
        status(409,a.call("POST","/subscriptions/analyze",Map.of("profile",p,"catalogVersion",UUID.randomUUID(),"revisions",Map.of(key,1)),true));
        for(int i=0;i<17;i++)status(200,a.call("POST","/subscriptions/analyze",input,true));
        status(429,a.call("POST","/subscriptions/analyze",input,true));
        assertEquals("no-store",compared.headers().firstValue("Cache-Control").orElseThrow());
    }


    Browser administrator() throws Exception {
        String address=email(); var b=verified(address);
        jdbc.update("UPDATE compass.user_account SET role='ADMIN' WHERE id=?",id(address));
        status(200,b.call("POST","/auth/login",credentials(address),true)); b.csrf(); return b;
    }
    String adminManifest() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        doc.put("versionId",UUID.randomUUID().toString()); return doc.toString();
    }
    @Test void adminDraftWorkflowRequiresReviewAndKeepsPublishedDataImmutable() throws Exception {
        var a=administrator(); String manifest=adminManifest();
        var created=a.call("POST","/admin/catalog/drafts",Map.of("manifest",manifest,"revision",0),true);
        status(200,created); String path="/admin/catalog/drafts/"+json.readTree(created.body()).get("id").asString();
        long active=context.getBean(CatalogPublicationService.class).current().revision();
        status(409,a.call("POST",path+"/actions",Map.of("action","PUBLISH","revision",1,"activeRevision",active),true));
        status(200,a.call("POST",path+"/actions",Map.of("action","VALIDATE","revision",1,"activeRevision",active),true));
        status(200,a.call("POST",path+"/actions",Map.of("action","REVIEW","revision",2,"activeRevision",active),true));
        status(409,a.call("POST",path+"/actions",Map.of("action","PUBLISH","revision",3,"activeRevision",active+1),true));
        status(200,a.call("POST",path+"/actions",Map.of("action","PUBLISH","revision",3,"activeRevision",active),true));
        status(409,a.call("PATCH",path,Map.of("manifest",manifest,"revision",4),true));
        status(409,a.call("POST",path+"/actions",Map.of("action","PUBLISH","revision",3,"activeRevision",active),true));
        status(200,a.call("GET","/admin/catalog/queue",null,false));
        assertTrue(json.readTree(a.call("GET","/admin/catalog/audit",null,false).body()).size()>=4);
    }
    @Test void adminRejectsUserCsrfAndInvalidManifestAndEditClearsApproval() throws Exception {
        var u=signedIn(email());
        status(403,u.call("GET","/admin/catalog/drafts",null,false));
        status(403,u.call("POST","/admin/catalog/drafts",Map.of("manifest",adminManifest(),"revision",0),true));
        var a=administrator();
        status(403,a.call("POST","/admin/catalog/drafts",Map.of("manifest","{}","revision",0),false));
        var created=a.call("POST","/admin/catalog/drafts",Map.of("manifest","{}","revision",0),true);
        status(200,created);String path="/admin/catalog/drafts/"+json.readTree(created.body()).get("id").asString();
        status(400,a.call("POST",path+"/actions",Map.of("action","VALIDATE","revision",1,"activeRevision",0),true));
        status(200,a.call("PATCH",path,Map.of("manifest",adminManifest(),"revision",1),true));
        status(200,a.call("POST",path+"/actions",Map.of("action","VALIDATE","revision",2,"activeRevision",0),true));
        status(200,a.call("POST",path+"/actions",Map.of("action","REVIEW","revision",3,"activeRevision",0),true));
        var edited=a.call("PATCH",path,Map.of("manifest",adminManifest(),"revision",4),true);status(200,edited);
        assertEquals("DRAFT",json.readTree(edited.body()).get("state").asString());
        status(409,a.call("POST",path+"/actions",Map.of("action","PUBLISH","revision",5,"activeRevision",0),true));
    }

    UUID publishFixture(tools.jackson.databind.node.ObjectNode doc) {
        UUID version=UUID.randomUUID();doc.put("versionId",version.toString());
        context.getBean(CatalogImportService.class).importManifest(doc.toString());
        var pub=context.getBean(CatalogPublicationService.class);
        pub.review(version,jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?",String.class,version),"monitor-test");
        pub.activate(version,pub.current().revision(),false,"monitor-test","Synthetic change");return version;
    }
    String watchFixture(Browser browser,Object profile,UUID version) throws Exception {
        UUID owner=UUID.fromString(json.readTree(browser.call("GET","/me",null,false).body()).get("id").asString());
        // Synthetic pre-existing beta right; grant authorization is exercised separately below.
        jdbc.update("INSERT INTO compass.beta_grant VALUES (?,now()+interval '1 day',false,1) ON CONFLICT DO NOTHING",owner);
        var response=browser.call("POST","/recommendations/preview",profile,true);status(200,response);var r=json.readTree(response.body());
        var saved=browser.call("POST","/recommendations",Map.of("profile",profile,"catalogVersion",version,"asOf",r.get("asOf").asString(),"engineVersion",r.get("engineVersion").asString(),"policyVersion",r.get("policyVersion").asString()),true);status(201,saved);
        var snap=json.readTree(saved.body());assertFalse(snap.at("/result/bundles").isEmpty());
        var stack=browser.call("POST","/saved-stacks",Map.of("snapshotId",snap.get("id").asString(),"bundleId",snap.at("/result/bundles/0/id").asString()),true);status(201,stack);
        var watch=browser.call("POST","/monitoring/watches",Map.of("stackId",json.readTree(stack.body()).get("id").asString(),"emailOptIn",true,"consent",true),true);status(200,watch);
        return json.readTree(watch.body()).get("id").asString();
    }
    @Test void monitoringOnlyImpactedOwnerGetsOneNoticeAndOutboxUnsubscribeIsConfirmed() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=publishFixture(doc);var a=signedIn(email());var b=signedIn(email());
        var editor=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/profiles/editor.json")));editor.putNull("asOf");
        String watch=watchFixture(a,editor,version);watchFixture(b,profile(),version);
        for(var offer:doc.get("priceOffers"))if(offer.get("id").asString().equals("creator-offer"))((tools.jackson.databind.node.ObjectNode)offer).put("amount","18").put("upfrontAmount","18");
        publishFixture(doc);
        var monitor=context.getBean(com.aicompass.compass.monitoring.MonitoringService.class);
        assertEquals(2,monitor.enqueue());assertEquals(0,monitor.enqueue());
        for(int n=0;n<2;n++){var job=monitor.claim();assertNotNull(job);monitor.process(job);monitor.process(job);}
        var notices=json.readTree(a.call("GET","/monitoring/notices",null,false).body());assertEquals(1,notices.size());
        assertEquals(0,json.readTree(b.call("GET","/monitoring/notices",null,false).body()).size());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM compass.monitor_job WHERE publication_revision=? AND recomputed",Integer.class,context.getBean(CatalogPublicationService.class).current().revision()));
        String notice=notices.get(0).get("id").asString();
        assertFalse(notices.get(0).at("/comparison/changes").isEmpty());
        status(404,b.call("PATCH","/monitoring/notices/"+notice,Map.of("appliedSelfReported",true),true));
        status(403,a.call("PATCH","/monitoring/notices/"+notice,Map.of("appliedSelfReported",true),false));
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        status(200,a.call("PATCH","/monitoring/notices/"+notice,Map.of("appliedSelfReported",true),true));
        assertEquals(List.of("CHANGE_NOTICE_VIEWED"),jdbc.queryForList("SELECT event_name FROM compass.product_event WHERE owner_id=?",String.class,jdbc.queryForObject("SELECT owner_id FROM compass.watched_stack WHERE id=CAST(? AS uuid)",UUID.class,watch)));
        status(404,b.call("PATCH","/monitoring/watches/"+watch,Map.of("active",false,"emailOptIn",false,"confirm",true),true));
        status(400,a.call("PATCH","/monitoring/watches/"+watch,Map.of("active",false,"emailOptIn",false,"confirm",false),true));
        status(200,a.call("PATCH","/monitoring/watches/"+watch,Map.of("active",false,"emailOptIn",false,"confirm",true),true));
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT state FROM compass.notification_outbox WHERE notice_id=CAST(? AS uuid)",String.class,notice));
        var export=a.call("GET","/me/export",null,false);status(200,export);
        assertEquals(1,json.readTree(export.body()).get("watches").size());
        assertEquals(1,json.readTree(export.body()).get("notices").size());
        // Seed a historical page boundary in this isolated fixture. Live delivery was exercised above.
        var publication=context.getBean(CatalogPublicationService.class);UUID latest=publication.current().versionId();
        for(int i=0;i<100;i++) {
            var event=publication.activate(i%2==0?version:latest,publication.current().revision(),false,"history-test","Synthetic history");
            jdbc.update("INSERT INTO compass.change_notice(id,owner_id,watch_id,publication_revision,payload) SELECT ?,owner_id,watch_id,?,payload FROM compass.change_notice WHERE id=CAST(? AS uuid)",UUID.randomUUID(),event.revision(),notice);
        }
        assertEquals(100,json.readTree(a.call("GET","/monitoring/notices",null,false).body()).size());
        assertEquals(101,json.readTree(a.call("GET","/me/export",null,false).body()).get("notices").size());
        UUID owner=UUID.fromString(json.readTree(a.call("GET","/me",null,false).body()).get("id").asString());
        status(204,a.call("DELETE","/me",Map.of("password",password,"confirm",true),true));
        for(String table:List.of("watched_stack","change_notice"))assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass."+table+" WHERE owner_id=?",Integer.class,owner));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.monitor_job WHERE watch_id=CAST(? AS uuid)",Integer.class,watch));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.notification_outbox WHERE notice_id=CAST(? AS uuid)",Integer.class,notice));
    }
    @Test void monitoringCrashLeaseRecoveryRetriesAndFencesOldWorkers() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=publishFixture(doc);var a=signedIn(email());String watch=watchFixture(a,profile(),version);publishFixture(doc);
        var monitor=context.getBean(com.aicompass.compass.monitoring.MonitoringService.class);monitor.enqueue();
        // Isolate this test's job; prior completed watch fixtures can also receive publications.
        jdbc.update("UPDATE compass.monitor_job SET state='DONE' WHERE watch_id<>CAST(? AS uuid)",watch);
        var abandoned=monitor.claim();assertNotNull(abandoned);assertNull(monitor.claim());
        jdbc.update("UPDATE compass.monitor_job SET lease_until=now()-interval '1 second' WHERE id=?",abandoned.id());
        var recovered=monitor.claim();assertEquals(abandoned.id(),recovered.id());assertNotEquals(abandoned.token(),recovered.token());
        monitor.process(abandoned);assertEquals("RUNNING",jdbc.queryForObject("SELECT state FROM compass.monitor_job WHERE id=?",String.class,recovered.id()));
        monitor.retry(recovered);assertNull(monitor.claim());
        jdbc.update("UPDATE compass.monitor_job SET available_at=now()-interval '1 second' WHERE id=?",recovered.id());
        var retried=monitor.claim();monitor.process(retried);assertNull(monitor.claim());
        assertEquals(0,json.readTree(a.call("GET","/monitoring/notices",null,false).body()).size());
        assertEquals(3,jdbc.queryForObject("SELECT attempts FROM compass.monitor_job WHERE id=?",Integer.class,retried.id()));
        jdbc.update("UPDATE compass.monitor_job SET state='READY',attempts=4,available_at=now()-interval '1 second' WHERE id=?",retried.id());
        var finalAttempt=monitor.claim();monitor.retry(finalAttempt);assertNull(monitor.claim());
        assertEquals("FAILED",jdbc.queryForObject("SELECT state FROM compass.monitor_job WHERE id=?",String.class,retried.id()));
    }

    @Test void adminDraftSizeLimitCountsUtf8BytesBeforeDatabaseConstraint() throws Exception {
        var a=administrator();
        status(413,a.call("POST","/admin/catalog/drafts",Map.of("manifest","я".repeat(2600000),"revision",0),true));
    }
    Map<String,Object> grant(String action,int days,long revision,UUID key) {
        return Map.of("action",action,"days",days,"expectedRevision",revision,"requestId",key);
    }
    @Test void betaGrantAuthorizationCsrfStrictJsonAndFakeSuccessNeverGrantRights() throws Exception {
        String address=email();var user=signedIn(address);var other=signedIn(email());var admin=administrator();
        String route="/admin/entitlements/"+id(address);var input=grant("GRANT",7,0,UUID.randomUUID());
        status(403,user.call("POST",route,input,true));status(403,other.call("GET",route,null,false));
        status(403,admin.call("POST",route,input,false));
        var injected=new HashMap<String,Object>(input);injected.put("plan","PREMIUM");status(400,admin.call("POST",route,injected,true));
        injected.remove("plan");injected.put("price","0");status(400,admin.call("POST",route,injected,true));
        status(400,admin.call("POST",route,grant("GRANT",91,0,UUID.randomUUID()),true));
        status(403,user.call("POST","/billing/success",Map.of("premium",true),true));
        assertEquals("FREE",json.readTree(user.call("GET","/me/entitlements?success=true&plan=PREMIUM",null,false).body()).get("plan").asString());
        status(403,user.call("POST","/monitoring/watches",Map.of("stackId",UUID.randomUUID(),"emailOptIn",false,"consent",true),true));
        status(200,admin.call("POST",route,input,true));
        assertEquals("PREMIUM",json.readTree(user.call("GET","/me",null,false).body()).get("plan").asString());
        assertEquals("FREE",json.readTree(other.call("GET","/me/entitlements",null,false).body()).get("plan").asString());
        status(204,user.call("POST","/auth/logout",null,true));status(401,user.call("GET","/me/entitlements",null,false));
    }
    @Test void betaConcurrentGrantsReplayRevocationExpirationAndProfileLimits() throws Exception {
        String address=email();var user=signedIn(address);var admin=administrator();UUID owner=id(address);
        String route="/admin/entitlements/"+owner;var first=grant("GRANT",7,0,UUID.randomUUID());var second=grant("GRANT",14,0,UUID.randomUUID());
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->admin.call("POST",route,first,true).statusCode());
            var b=pool.submit(()->admin.call("POST",route,second,true).statusCode());
            var codes=List.of(a.get(),b.get());assertTrue(codes.contains(200)&&codes.contains(409));
        }
        UUID key=jdbc.queryForObject("SELECT request_id FROM compass.beta_grant_audit WHERE owner_id=?",UUID.class,owner);
        var winner=key.equals(first.get("requestId"))?first:second;
        status(200,admin.call("POST",route,winner,true));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM compass.beta_grant_audit WHERE owner_id=?",Integer.class,owner));
        Object profile=profile();var body=Map.of("name","Beta profile","profile",profile,"revision",0);
        for(int n=0;n<4;n++)status(201,user.call("POST","/profiles",body,true));
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->user.call("POST","/profiles",body,true).statusCode());
            var b=pool.submit(()->user.call("POST","/profiles",body,true).statusCode());
            var codes=List.of(a.get(),b.get());assertTrue(codes.contains(201)&&codes.contains(409));
        }
        status(200,admin.call("POST",route,grant("REVOKE",0,1,UUID.randomUUID()),true));
        status(200,admin.call("POST",route,winner,true)); // old replay must not reactivate
        assertEquals("FREE",json.readTree(user.call("GET","/me/entitlements",null,false).body()).get("plan").asString());
        status(409,user.call("POST","/profiles",body,true));
        status(200,admin.call("POST",route,grant("GRANT",1,2,UUID.randomUUID()),true));
        jdbc.update("UPDATE compass.beta_grant SET expires_at=now()-interval '1 second' WHERE owner_id=?",owner);
        var rights=json.readTree(user.call("GET","/me/entitlements",null,false).body());assertEquals("EXPIRED",rights.get("grantState").asString());assertEquals("FREE",rights.get("plan").asString());
        status(403,user.call("POST","/monitoring/watches",Map.of("stackId",UUID.randomUUID(),"emailOptIn",false,"consent",true),true));
        var exported=json.readTree(user.call("GET","/me/export",null,false).body());assertEquals(5,exported.get("profiles").size());assertEquals(3,exported.get("entitlementAudit").size());
        status(204,user.call("DELETE","/me",Map.of("password",password,"confirm",true),true));
        for(String table:List.of("beta_grant","beta_grant_audit"))assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass."+table+" WHERE owner_id=?",Integer.class,owner));
    }
    @Test void betaHistoryIsServerGatedButExportRemainsCompleteAfterExpiry() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=publishFixture(doc);String address=email();var user=signedIn(address);var other=signedIn(email());var admin=administrator();
        Object p=profile();var preview=json.readTree(user.call("POST","/recommendations/preview",p,true).body());
        var body=Map.of("profile",p,"catalogVersion",version,"asOf",preview.get("asOf").asString(),"engineVersion",preview.get("engineVersion").asString(),"policyVersion",preview.get("policyVersion").asString());
        var first=user.call("POST","/recommendations",body,true);status(201,first);String old=json.readTree(first.body()).get("id").asString();
        status(201,user.call("POST","/recommendations",body,true));
        assertEquals(1,json.readTree(user.call("GET","/recommendations",null,false).body()).size());
        status(403,user.call("GET","/recommendations/"+old,null,false));status(404,other.call("GET","/recommendations/"+old,null,false));
        status(200,admin.call("POST","/admin/entitlements/"+id(address),grant("GRANT",1,0,UUID.randomUUID()),true));
        assertEquals(2,json.readTree(user.call("GET","/recommendations",null,false).body()).size());status(200,user.call("GET","/recommendations/"+old,null,false));
        jdbc.update("UPDATE compass.beta_grant SET expires_at=now()-interval '1 second' WHERE owner_id=?",id(address));
        status(403,user.call("GET","/recommendations/"+old,null,false));
        assertEquals(2,json.readTree(user.call("GET","/me/export",null,false).body()).get("recommendations").size());
        status(204,user.call("DELETE","/recommendations/"+old,null,true));
    }
    @Test void revokedOrExpiredBetaStopsClaimedMonitoringJobAndCancelsOutbox() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=publishFixture(doc);String address=email();var user=signedIn(address);String watch=watchFixture(user,profile(),version);
        publishFixture(doc);var monitor=context.getBean(com.aicompass.compass.monitoring.MonitoringService.class);monitor.enqueue();
        jdbc.update("UPDATE compass.monitor_job SET state='DONE' WHERE watch_id<>CAST(? AS uuid)",watch);
        var job=monitor.claim();assertNotNull(job);
        var admin=administrator();status(200,admin.call("POST","/admin/entitlements/"+id(address),grant("REVOKE",0,1,UUID.randomUUID()),true));
        monitor.process(job);assertEquals("DONE",jdbc.queryForObject("SELECT state FROM compass.monitor_job WHERE id=?",String.class,job.id()));
        assertFalse(jdbc.queryForObject("SELECT recomputed FROM compass.monitor_job WHERE id=?",Boolean.class,job.id()));
        assertFalse(json.readTree(user.call("GET","/monitoring/watches",null,false).body()).get(0).get("effectiveActive").asBoolean());
        status(403,user.call("PATCH","/monitoring/watches/"+watch,Map.of("active",true,"emailOptIn",false,"confirm",true),true));
        status(200,user.call("PATCH","/monitoring/watches/"+watch,Map.of("active",false,"emailOptIn",false,"confirm",true),true));
        assertEquals(0,json.readTree(user.call("GET","/monitoring/notices",null,false).body()).size());
    }

    @Test void analyticsOptInStrictFieldsOwnershipWithdrawalAndDelete() throws Exception {
        String address=email();var a=signedIn(address);var b=signedIn(email());UUID owner=id(address);
        assertFalse(json.readTree(a.call("GET","/me/analytics",null,false).body()).get("enabled").asBoolean());
        status(200,a.call("POST","/me/analytics/revisit",Map.of("confirm",true),true));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.product_event WHERE owner_id=?",Integer.class,owner));
        status(403,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),false));
        status(400,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",false),true));
        status(400,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true,"email",address),true));
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        status(400,a.call("POST","/me/analytics/revisit",Map.of("confirm",true,"event","PAYMENT_CONFIRMED"),true));
        for(int n=0;n<2;n++)status(200,a.call("POST","/me/analytics/revisit",Map.of("confirm",true),true));
        assertEquals(1,json.readTree(a.call("GET","/me/analytics",null,false).body()).get("events").size());
        assertEquals(0,json.readTree(b.call("GET","/me/analytics?owner="+owner,null,false).body()).get("events").size());
        assertEquals(1,json.readTree(a.call("GET","/me/export",null,false).body()).at("/analytics/events").size());
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",false,"confirm",true),true));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.product_event WHERE owner_id=?",Integer.class,owner));
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        status(200,a.call("POST","/me/analytics/revisit",Map.of("confirm",true),true));
        status(204,a.call("DELETE","/me",Map.of("password",password,"confirm",true),true));
        for(String table:List.of("analytics_consent","product_event"))assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass."+table+" WHERE owner_id=?",Integer.class,owner));
    }
    @Test void analyticsConcurrencyDoesNotOutrunWithdrawalAndRetentionPurgesOldDays() throws Exception {
        String address=email();var a=signedIn(address);UUID owner=id(address);
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var record=pool.submit(()->a.call("POST","/me/analytics/revisit",Map.of("confirm",true),true).statusCode());
            var withdraw=pool.submit(()->a.call("PATCH","/me/analytics",Map.of("enabled",false,"confirm",true),true).statusCode());
            assertEquals(200,record.get());assertEquals(200,withdraw.get());
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.product_event WHERE owner_id=?",Integer.class,owner));
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        status(200,a.call("POST","/me/analytics/revisit",Map.of("confirm",true),true));
        jdbc.update("INSERT INTO compass.product_event VALUES (?,'REVISIT',current_date-40)",owner);
        assertEquals(1,json.readTree(a.call("GET","/me/analytics",null,false).body()).get("events").size());
        context.getBean(com.aicompass.compass.analytics.UsageAnalyticsService.class).purgeExpired();
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM compass.product_event WHERE owner_id=?",Integer.class,owner));
    }
    @Test void analyticsRecordsServerConfirmedSaveOnlyAfterConsentWithoutProfilePayload() throws Exception {
        var doc=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(Path.of("../data/fixtures/recommendation-demo.json")));
        UUID version=publishFixture(doc);String address=email();var a=signedIn(address);Object p=profile();
        var preview=json.readTree(a.call("POST","/recommendations/preview",p,true).body());
        var body=Map.of("profile",p,"catalogVersion",version,"asOf",preview.get("asOf").asString(),"engineVersion",preview.get("engineVersion").asString(),"policyVersion",preview.get("policyVersion").asString());
        status(201,a.call("POST","/recommendations",body,true));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM compass.product_event WHERE owner_id=?",Integer.class,id(address)));
        status(200,a.call("PATCH","/me/analytics",Map.of("enabled",true,"confirm",true),true));
        status(201,a.call("POST","/recommendations",body,true));status(201,a.call("POST","/recommendations",body,true));
        assertEquals(List.of("RESULT_SAVED"),jdbc.queryForList("SELECT event_name FROM compass.product_event WHERE owner_id=?",String.class,id(address)));
        assertEquals(List.of("owner_id","event_name","event_day"),jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='compass' AND table_name='product_event' ORDER BY ordinal_position",String.class));
    }

}
