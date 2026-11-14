package com.aicompass.compass.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EntitlementService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public EntitlementService(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    public record Entitlements(String plan, int profileLimit, int watchLimit, boolean history,
            @Schema(nullable=true) Instant expiresAt, long revision, String grantState,
            String paymentMode, String feeAmount, String feeCurrency) {}
    public record BetaGrantAudit(UUID requestId, String action, long revision, Instant expiresAt, Instant createdAt) {}
    public void lockOwner(UUID owner) {
        if(jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND verified_at IS NOT NULL FOR UPDATE",UUID.class,owner).isEmpty())
            throw error(HttpStatus.NOT_FOUND,"Account not found");
    }
    public Entitlements current(UUID owner) {
        var rows=jdbc.queryForList("SELECT * FROM compass.beta_grant WHERE owner_id=?",owner);
        if(rows.isEmpty()) return rights(false,null,0,"NONE");
        var row=rows.getFirst(); Instant expiry=((Timestamp)row.get("expires_at")).toInstant();
        boolean revoked=Boolean.TRUE.equals(row.get("revoked"));
        boolean active=!revoked && expiry.isAfter(clock.instant());
        return rights(active,expiry,((Number)row.get("revision")).longValue(),revoked?"REVOKED":active?"ACTIVE":"EXPIRED");
    }
    private Entitlements rights(boolean active,Instant expiry,long revision,String state) {
        return new Entitlements(active?"PREMIUM":"FREE",active?5:1,active?5:0,active,expiry,revision,state,"OFF","0.00","USD");
    }
    public void requirePremium(UUID owner) {
        if(!current(owner).history()) throw error(HttpStatus.FORBIDDEN,"Active Premium beta grant required");
    }
    public void authorize(UUID actor) {
        if(jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND role='ADMIN' AND verified_at IS NOT NULL",UUID.class,actor).isEmpty())
            throw error(HttpStatus.FORBIDDEN,"Administrator required");
    }
    public Entitlements inspect(UUID actor,UUID owner) { authorize(actor);lockOwner(owner);return current(owner); }
    public List<BetaGrantAudit> audit(UUID owner) {
        return jdbc.query("SELECT * FROM compass.beta_grant_audit WHERE owner_id=? ORDER BY created_at,request_id",(r,n)->
                new BetaGrantAudit(r.getObject("request_id",UUID.class),r.getString("action"),r.getLong("result_revision"),r.getTimestamp("expires_at").toInstant(),r.getTimestamp("created_at").toInstant()),owner);
    }
    public Entitlements change(UUID actor,UUID owner,UUID request,String action,int days,long expected) {
        authorize(actor);lockOwner(owner);
        if(!Set.of("GRANT","REVOKE").contains(action) || (action.equals("GRANT")?(days<1||days>90):days!=0))
            throw error(HttpStatus.BAD_REQUEST,"Grant requires 1–90 days; revocation requires zero days");
        var previous=jdbc.queryForList("SELECT * FROM compass.beta_grant_audit WHERE request_id=?",request);
        if(!previous.isEmpty()) {
            var p=previous.getFirst();
            if(!owner.equals(p.get("owner_id")) || !actor.equals(p.get("actor_id")) || !action.equals(p.get("action"))
                    || days!=((Number)p.get("days")).intValue() || expected!=((Number)p.get("expected_revision")).longValue())
                throw error(HttpStatus.CONFLICT,"Request key already used");
            // Replay observes current rights; it cannot resurrect a later revoked grant.
            return current(owner);
        }
        var old=current(owner);
        if(old.revision()!=expected) throw error(HttpStatus.CONFLICT,"Grant changed; reload before editing");
        if(action.equals("REVOKE") && old.revision()==0) throw error(HttpStatus.CONFLICT,"No grant to revoke");
        Instant expires=action.equals("GRANT")?clock.instant().plus(Duration.ofDays(days)):old.expiresAt();
        // Global idempotency keys also protect concurrent requests for different owners.
        int inserted=jdbc.update("INSERT INTO compass.beta_grant_audit(request_id,owner_id,actor_id,action,days,expected_revision,result_revision,expires_at) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                request,owner,actor,action,days,expected,expected+1,Timestamp.from(expires));
        if(inserted!=1) throw error(HttpStatus.CONFLICT,"Request key already used");
        jdbc.update("INSERT INTO compass.beta_grant(owner_id,expires_at,revoked,revision) VALUES (?,?,?,?) ON CONFLICT(owner_id) DO UPDATE SET expires_at=EXCLUDED.expires_at,revoked=EXCLUDED.revoked,revision=EXCLUDED.revision",
                owner,Timestamp.from(expires),action.equals("REVOKE"),expected+1);
        if(action.equals("REVOKE")) jdbc.update("UPDATE compass.notification_outbox SET state='CANCELLED' WHERE notice_id IN (SELECT id FROM compass.change_notice WHERE owner_id=?)",owner);
        return current(owner);
    }
    private static ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
