package com.aicompass.compass.monitoring;

import com.aicompass.compass.catalog.*;
import com.aicompass.compass.identity.IdentityJson;
import com.aicompass.compass.portfolio.PortfolioService;
import com.aicompass.compass.recommendation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MonitoringService {
    private final com.aicompass.compass.billing.EntitlementService entitlements;
    private final com.aicompass.compass.analytics.UsageAnalyticsService analytics;
    private final JdbcTemplate jdbc;
    private final IdentityJson json;
    private final PortfolioService portfolio;
    private final CatalogReadService catalog;
    private final CatalogPublicationService publication;
    private final Clock clock;
    public MonitoringService(com.aicompass.compass.analytics.UsageAnalyticsService analytics,com.aicompass.compass.billing.EntitlementService entitlements, JdbcTemplate jdbc, IdentityJson json, PortfolioService portfolio,
            CatalogReadService catalog, CatalogPublicationService publication, Clock clock) {
        this.analytics=analytics; this.entitlements=entitlements; this.jdbc=jdbc; this.json=json; this.portfolio=portfolio; this.catalog=catalog; this.publication=publication; this.clock=clock;
    }
    public record Watch(UUID id, UUID stackId, boolean active, boolean emailOptIn, boolean effectiveActive) {}
    public record Comparison(UUID previousVersion, UUID currentVersion, List<CatalogChanges.Change> changes,
            RecommendationResult before, RecommendationResult after, String explanation) {}
    public record Notice(UUID id, UUID watchId, long publicationRevision, Comparison comparison,
            boolean read, boolean appliedSelfReported, Instant createdAt, int horizonMonths) {}
    public record Job(long id, UUID token) {}

    public List<Watch> watches(UUID owner) {
        return jdbc.query("SELECT * FROM compass.watched_stack WHERE owner_id=? ORDER BY created_at,id", (r,n) ->
                new Watch(r.getObject("id",UUID.class),r.getObject("stack_id",UUID.class),r.getBoolean("active"),r.getBoolean("email_opt_in"),r.getBoolean("active") && entitlements.current(owner).history()), owner);
    }
    public Watch watch(UUID owner, UUID stack, boolean emailOptIn) {
        if (jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND verified_at IS NOT NULL FOR UPDATE",UUID.class,owner).isEmpty()) throw missing();
        entitlements.requirePremium(owner);
        if (portfolio.stacks(owner).stream().noneMatch(s -> s.id().equals(stack))) throw missing();
        var existing = watches(owner).stream().filter(w -> w.stackId().equals(stack)).findFirst();
        if (existing.isPresent()) return existing.get();
        if (watches(owner).size() >= 5) throw new ResponseStatusException(HttpStatus.CONFLICT,"Watch limit reached");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO compass.watched_stack(id,owner_id,stack_id,start_revision,email_opt_in) VALUES (?,?,?,?,?)",
                id,owner,stack,publication.current().revision(),emailOptIn);
        return new Watch(id,stack,true,emailOptIn,true);
    }
    public Watch preferences(UUID owner, UUID id, boolean active, boolean email, boolean confirmed) {
        if (!confirmed) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm notification preferences");
        entitlements.lockOwner(owner);
        if(active) entitlements.requirePremium(owner);
        // Row lock serializes unsubscribe with delivery: no email outbox survives a confirmed opt-out.
        if (jdbc.queryForList("SELECT id FROM compass.watched_stack WHERE id=? AND owner_id=? FOR UPDATE",UUID.class,id,owner).isEmpty()) throw missing();
        jdbc.update("UPDATE compass.watched_stack SET start_revision=CASE WHEN NOT active AND ? THEN (SELECT revision FROM compass.catalog_active WHERE singleton) ELSE start_revision END,active=?,email_opt_in=? WHERE id=? AND owner_id=?",active,active,email,id,owner);
        if (!active || !email) jdbc.update("UPDATE compass.notification_outbox SET state='CANCELLED' WHERE notice_id IN (SELECT id FROM compass.change_notice WHERE watch_id=? AND owner_id=?)",id,owner);
        return watches(owner).stream().filter(w -> w.id().equals(id)).findFirst().orElseThrow();
    }
    public List<Notice> notices(UUID owner) { return readNotices(owner, true); }
    public List<Notice> exportNotices(UUID owner) { return readNotices(owner, false); }
    private List<Notice> readNotices(UUID owner, boolean page) {
        return jdbc.query("""
                SELECT n.*, (r.profile_json->>'horizonMonths')::integer AS horizon_months
                FROM compass.change_notice n JOIN compass.watched_stack w ON w.id=n.watch_id
                JOIN compass.saved_stack s ON s.id=w.stack_id
                JOIN compass.recommendation_snapshot r ON r.id=s.snapshot_id
                WHERE n.owner_id=? ORDER BY n.created_at DESC,n.id
                """ + (page ? " LIMIT 100" : ""), (r,n) ->
                new Notice(r.getObject("id",UUID.class),r.getObject("watch_id",UUID.class),r.getLong("publication_revision"),
                        json.read(r.getString("payload"),Comparison.class),r.getTimestamp("read_at")!=null,r.getTimestamp("applied_at")!=null,r.getTimestamp("created_at").toInstant(),r.getInt("horizon_months")),owner);
    }
    public void acknowledge(UUID owner, UUID id, boolean applied) {
        entitlements.lockOwner(owner);
        if (jdbc.update("UPDATE compass.change_notice SET read_at=COALESCE(read_at,?),applied_at=CASE WHEN ? THEN COALESCE(applied_at,?) ELSE applied_at END WHERE id=? AND owner_id=?",
                Timestamp.from(clock.instant()),applied,Timestamp.from(clock.instant()),id,owner)!=1) throw missing();
        analytics.record(owner,com.aicompass.compass.analytics.UsageAnalyticsService.Event.CHANGE_NOTICE_VIEWED);
    }
    /** Durable publication log is the event source; unique (watch,revision) is the replay key. */
    public int enqueue() {
        return jdbc.update("""
                INSERT INTO compass.monitor_job(watch_id,publication_revision)
                SELECT w.id,p.revision FROM compass.watched_stack w JOIN compass.catalog_publication p ON p.revision>w.start_revision
                WHERE w.active AND p.previous_version_id IS NOT NULL
                AND EXISTS (SELECT 1 FROM compass.beta_grant g WHERE g.owner_id=w.owner_id AND NOT g.revoked AND g.expires_at>CURRENT_TIMESTAMP)
                AND NOT EXISTS (SELECT 1 FROM compass.monitor_job j WHERE j.watch_id=w.id AND j.publication_revision=p.revision)
                ORDER BY p.revision,w.id LIMIT 100 ON CONFLICT DO NOTHING
                """);
    }
    public Job claim() {
        var now=Timestamp.from(clock.instant());
        // A process killed after claim leaves a recoverable lease. Tokens fence stale workers.
        jdbc.update("UPDATE compass.monitor_job SET state='FAILED' WHERE state='RUNNING' AND lease_until<=? AND attempts>=5",now);
        var ids=jdbc.queryForList("""
                SELECT j.id FROM compass.monitor_job j WHERE j.attempts<5 AND
                ((j.state='READY' AND j.available_at<=?) OR (j.state='RUNNING' AND j.lease_until<=?))
                AND EXISTS (SELECT 1 FROM compass.watched_stack w WHERE w.id=j.watch_id AND (w.last_recomputed_at IS NULL OR w.last_recomputed_at<=CURRENT_TIMESTAMP-interval '1 minute'))
                AND NOT EXISTS (SELECT 1 FROM compass.monitor_job earlier WHERE earlier.watch_id=j.watch_id
                    AND earlier.publication_revision<j.publication_revision AND earlier.state IN ('READY','RUNNING'))
                ORDER BY j.publication_revision,j.id LIMIT 1 FOR UPDATE SKIP LOCKED
                """,Long.class,now,now);
        if(ids.isEmpty()) return null;
        UUID token=UUID.randomUUID(); long id=ids.getFirst();
        jdbc.update("UPDATE compass.monitor_job SET state='RUNNING',attempts=attempts+1,lease_token=?,lease_until=? WHERE id=?",token,Timestamp.from(clock.instant().plusSeconds(120)),id);
        return new Job(id,token);
    }
    public void retry(Job job) {
        jdbc.update("UPDATE compass.monitor_job SET state=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'READY' END,available_at=?,lease_until=NULL WHERE id=? AND lease_token=? AND state='RUNNING'",
                Timestamp.from(clock.instant().plusSeconds(60)),job.id(),job.token());
    }
    public void process(Job job) {
        // Lock owner before job/watch, matching account deletion and grant revocation order.
        var owners=jdbc.queryForList("SELECT w.owner_id FROM compass.monitor_job j JOIN compass.watched_stack w ON w.id=j.watch_id WHERE j.id=?",UUID.class,job.id());
        if(owners.isEmpty()) return;
        if(jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? FOR UPDATE",UUID.class,owners.getFirst()).isEmpty()) return;
        var jobs=jdbc.queryForList("SELECT * FROM compass.monitor_job WHERE id=? AND lease_token=? AND state='RUNNING' FOR UPDATE",job.id(),job.token());
        if(jobs.isEmpty()) return;
        UUID watchId=(UUID)jobs.getFirst().get("watch_id"); long revision=((Number)jobs.getFirst().get("publication_revision")).longValue();
        var watches=jdbc.queryForList("SELECT * FROM compass.watched_stack WHERE id=? FOR UPDATE",watchId);
        if(watches.isEmpty()) return;
        var watch=watches.getFirst(); UUID owner=(UUID)watch.get("owner_id");
        if(Boolean.TRUE.equals(watch.get("active")) && entitlements.current(owner).history()) {
            var stack=portfolio.stacks(owner).stream().filter(s -> s.id().equals(watch.get("stack_id"))).findFirst().orElseThrow();
            var saved=portfolio.recommendation(owner,stack.snapshotId());
            var event=jdbc.queryForMap("SELECT previous_version_id,version_id FROM compass.catalog_publication WHERE revision=?",revision);
            var before=catalog.snapshot((UUID)event.get("previous_version_id")); var after=catalog.snapshot((UUID)event.get("version_id"));
            var bundle=java.util.stream.Stream.concat(saved.result().bundles().stream(),saved.result().conditionalAlternatives().stream()).filter(b -> b.id().equals(stack.bundleId())).findFirst().orElseThrow();
            var planIds=new HashSet<String>(); bundle.items().stream().map(RecommendationResult.Item::planId).filter(Objects::nonNull).forEach(planIds::add);
            var diff=new CatalogChanges();
            var changes=diff.compare(before,after,saved.profile(),planIds,clock.instant());
            if(!changes.isEmpty()) {
                Instant asOf=clock.instant();
                var engine=new RecommendationEngine();
                var oldResult=engine.calculate(saved.profile(),before,asOf);
                var newResult=engine.calculate(saved.profile(),after,asOf);
                var comparison=new Comparison(before.version(),after.version(),changes,
                        oldResult,newResult,
                        "Изменились условия каталога для сохранённых потребностей. Сравнение рассчитано на одну дату. Это не покупка, отмена или подтверждённая экономия; UNKNOWN требует проверки источника.");
                if (diff.significant(changes,before,after,oldResult,newResult,asOf) && entitlements.current(owner).history()) {
                UUID notice=UUID.randomUUID();
                int added=jdbc.update("INSERT INTO compass.change_notice(id,owner_id,watch_id,publication_revision,payload) VALUES (?,?,?,?,CAST(? AS jsonb)) ON CONFLICT DO NOTHING",
                        notice,owner,watchId,revision,json.write(comparison));
                if(added==1 && Boolean.TRUE.equals(watch.get("email_opt_in"))) jdbc.update("INSERT INTO compass.notification_outbox(notice_id) VALUES (?) ON CONFLICT DO NOTHING",notice);
                }
                jdbc.update("UPDATE compass.monitor_job SET recomputed=true WHERE id=?",job.id());
                jdbc.update("UPDATE compass.watched_stack SET last_recomputed_at=? WHERE id=?",Timestamp.from(clock.instant()),watchId);
            }
        }
        jdbc.update("UPDATE compass.monitor_job SET state='DONE',lease_until=NULL WHERE id=? AND lease_token=?",job.id(),job.token());
    }
    private static ResponseStatusException missing() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");}
}
