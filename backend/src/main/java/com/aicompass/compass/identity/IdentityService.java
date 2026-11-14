package com.aicompass.compass.identity;

import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.aicompass.compass.identity.AuthDtos.*;

@Service
public class IdentityService {
    private final com.aicompass.compass.billing.EntitlementService entitlements;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authentication;
    private final LocalMailSink mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public IdentityService(com.aicompass.compass.billing.EntitlementService entitlements, JdbcTemplate jdbc, PasswordEncoder passwords, AuthenticationManager authentication, LocalMailSink mail, Clock clock) {
        this.entitlements=entitlements; this.jdbc=jdbc; this.passwords=passwords; this.authentication=authentication; this.mail=mail; this.clock=clock;
    }
    public static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public record LoginIdentity(UUID id, String role, long credentialVersion) {}
    public Account account(UUID id) {
        return jdbc.query("SELECT * FROM compass.user_account WHERE id=?", (r,n) -> new Account(id,r.getString("email"),r.getString("role"),r.getTimestamp("verified_at")!=null,entitlements.current(id).plan(),r.getTimestamp("created_at").toInstant()), id)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
    @Transactional
    public void register(Credentials input) {
        mail.requireLocal();
        String hash = passwords.encode(input.password()); // Also performed for duplicates.
        UUID id=UUID.randomUUID(); var now=Timestamp.from(clock.instant());
        int inserted=jdbc.update("INSERT INTO compass.user_account(id,email,password_hash,created_at,updated_at) VALUES (?,?,?,?,?) ON CONFLICT(email) DO NOTHING",id,normalize(input.email()),hash,now,now);
        if (inserted==1) issue(id,normalize(input.email()),"VERIFY");
    }
    @Transactional
    public LoginIdentity login(Credentials input) {
        // Serialize verification/reset/login for an account; the version also rejects late session saves after reset.
        jdbc.queryForList("SELECT id FROM compass.user_account WHERE email=? FOR UPDATE",normalize(input.email()));
        try {
            var auth=authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(normalize(input.email()),input.password()));
            UUID id=UUID.fromString(auth.getName());
            var a=account(id);
            if(!a.verified()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password not accepted");
            return new LoginIdentity(id,a.role(),jdbc.queryForObject("SELECT credential_version FROM compass.user_account WHERE id=?",Long.class,id));
        } catch (AuthenticationException error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password not accepted"); }
    }
    @Transactional
    public void requestEmail(String email, String purpose) {
        mail.requireLocal();
        var rows=jdbc.queryForList("SELECT id,verified_at FROM compass.user_account WHERE email=? FOR UPDATE",normalize(email));
        if (!rows.isEmpty() && (purpose.equals("RESET") || rows.getFirst().get("verified_at")==null))
            issue((UUID)rows.getFirst().get("id"),normalize(email),purpose);
    }
    private void issue(UUID id,String email,String purpose) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("DELETE FROM compass.identity_token WHERE user_id=? AND purpose=?",id,purpose);
        jdbc.update("INSERT INTO compass.identity_token VALUES (?,?,?,?)",AuthRateLimiter.hash(token),id,purpose,Timestamp.from(clock.instant().plusSeconds(purpose.equals("VERIFY")?86400:1800)));
        mail.send(email,purpose,token);
    }
    @Transactional
    public void consume(String token,String purpose,String password) {
        var ids=jdbc.queryForList("SELECT a.id FROM compass.user_account a JOIN compass.identity_token t ON t.user_id=a.id WHERE t.token_hash=? AND t.purpose=? AND t.expires_at>? FOR UPDATE OF a",UUID.class,AuthRateLimiter.hash(token),purpose,Timestamp.from(clock.instant()));
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link is invalid or expired");
        UUID id=ids.getFirst();
        if(jdbc.update("DELETE FROM compass.identity_token WHERE token_hash=? AND purpose=? AND expires_at>?",AuthRateLimiter.hash(token),purpose,Timestamp.from(clock.instant()))!=1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link is invalid or expired");
        if(purpose.equals("VERIFY")) jdbc.update("UPDATE compass.user_account SET verified_at=?,updated_at=? WHERE id=?",Timestamp.from(clock.instant()),Timestamp.from(clock.instant()),id);
        else {
            jdbc.update("UPDATE compass.user_account SET password_hash=?,credential_version=credential_version+1,updated_at=? WHERE id=?",passwords.encode(password),Timestamp.from(clock.instant()),id);
            jdbc.update("DELETE FROM compass.spring_session WHERE principal_name=?",id.toString());
        }
    }
    @Transactional
    public void delete(UUID id, String password) {
        var hashes=jdbc.queryForList("SELECT password_hash FROM compass.user_account WHERE id=? FOR UPDATE",String.class,id);
        if(hashes.isEmpty() || !passwords.matches(password,hashes.getFirst())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Confirmation failed");
        jdbc.update("DELETE FROM compass.spring_session WHERE principal_name=?",id.toString());
        String email=account(id).email();
        for(String purpose:List.of("register:","login:","VERIFY:","RESET:"))
            jdbc.update("DELETE FROM compass.auth_rate WHERE bucket_hash=?",AuthRateLimiter.hash(purpose+email));
        for(String purpose:List.of("delete:","save:","optimize:","watch:","admin:","grant:","analytics:"))
            jdbc.update("DELETE FROM compass.auth_rate WHERE bucket_hash=?",AuthRateLimiter.hash(purpose+id));
        mail.removeFor(email);
        jdbc.update("DELETE FROM compass.user_account WHERE id=?",id);
    }
}
