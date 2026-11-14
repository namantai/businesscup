package com.aicompass.compass.catalog;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CatalogAdminService {
    private final JdbcTemplate jdbc;
    private final CatalogImportService imports;
    private final CatalogPublicationService publication;
    private final CatalogReadService catalog;

    public CatalogAdminService(JdbcTemplate jdbc, CatalogImportService imports,
            CatalogPublicationService publication, CatalogReadService catalog) {
        this.jdbc = jdbc;
        this.imports = imports;
        this.publication = publication;
        this.catalog = catalog;
    }

    public record Draft(UUID id, String manifest, long revision, String state,
            @io.swagger.v3.oas.annotations.media.Schema(nullable = true) UUID versionId) {}
    public record Audit(long id, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) UUID actorId,
            String action, UUID subjectId, long revision, Instant createdAt) {}

    public void authorize(UUID actor) {
        if (jdbc.queryForObject("SELECT count(*) FROM compass.user_account WHERE id=? AND role='ADMIN' AND verified_at IS NOT NULL",
                Integer.class, actor) != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator required");
    }

    public List<Draft> drafts(UUID actor) {
        authorize(actor);
        return jdbc.query("SELECT * FROM compass.catalog_draft ORDER BY updated_at DESC LIMIT 50", (r, n) ->
                new Draft(r.getObject("id", UUID.class), r.getString("manifest_text"), r.getLong("revision"),
                        r.getString("state"), r.getObject("version_id", UUID.class)));
    }

    private Draft draft(UUID id, long revision) {
        var rows = jdbc.query("SELECT * FROM compass.catalog_draft WHERE id=? FOR UPDATE", (r, n) ->
                new Draft(id, r.getString("manifest_text"), r.getLong("revision"), r.getString("state"),
                        r.getObject("version_id", UUID.class)), id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found");
        if (rows.getFirst().revision() != revision) throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft changed; reload");
        return rows.getFirst();
    }

    public Draft create(UUID actor, String manifest) {
        authorize(actor);
        checkSize(manifest);
        if (jdbc.queryForObject("SELECT count(*) FROM compass.catalog_draft WHERE state<>'PUBLISHED'", Integer.class) >= 50)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft limit reached");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO compass.catalog_draft(id,author_id,manifest_text) VALUES (?,?,?)", id, actor, manifest);
        audit(actor, "DRAFT_CREATED", id, 1);
        return draft(id, 1);
    }

    public Draft edit(UUID actor, UUID id, long revision, String manifest) {
        authorize(actor);
        checkSize(manifest);
        if (draft(id, revision).state().equals("PUBLISHED")) throw new ResponseStatusException(HttpStatus.CONFLICT, "Published draft is immutable");
        jdbc.update("UPDATE compass.catalog_draft SET manifest_text=?,revision=revision+1,state='DRAFT',version_id=NULL,reviewed_by=NULL,updated_at=now() WHERE id=?", manifest, id);
        audit(actor, "DRAFT_EDITED", id, revision + 1);
        return draft(id, revision + 1);
    }

    public Draft advance(UUID actor, UUID id, long revision, String action, long activeRevision) {
        authorize(actor);
        Draft d = draft(id, revision);
        String next;
        UUID version = d.versionId();
        try {
            switch (action) {
                case "VALIDATE" -> {
                    requireState(d, "DRAFT");
                    version = imports.importManifest(d.manifest());
                    next = "VALIDATED";
                }
                case "REVIEW" -> {
                    requireState(d, "VALIDATED");
                    String hash = jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id=?", String.class, version);
                    publication.review(version, hash, actor.toString());
                    next = "REVIEWED";
                }
                case "PUBLISH" -> {
                    requireState(d, "REVIEWED");
                    publication.activate(version, activeRevision, false, actor.toString(), "Reviewed admin draft " + id);
                    next = "PUBLISHED";
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catalog validation failed; check manifest fields and evidence");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication changed; reload before publishing");
        }
        jdbc.update("UPDATE compass.catalog_draft SET state=?,version_id=?,revision=revision+1,reviewed_by=CASE WHEN ?='REVIEWED' THEN ? ELSE reviewed_by END,updated_at=now() WHERE id=?",
                next, version, next, actor, id);
        audit(actor, action, id, revision + 1);
        return draft(id, revision + 1);
    }

    private void checkSize(String manifest) {
        if (manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 5000000)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Manifest exceeds 5 MB");
    }

    private void requireState(Draft d, String expected) {
        if (!d.state().equals(expected)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid draft transition");
    }

    public CatalogPublicationService.Active active(UUID actor) { authorize(actor); return publication.current(); }
    public List<CatalogViews.Evidence> queue(UUID actor) {
        authorize(actor);
        var current = publication.current();
        if (current.versionId() == null) return List.of();
        return catalog.snapshot(current.versionId()).evidence().stream()
                .filter(e -> Set.of("STALE", "CONFLICTED", "UNKNOWN").contains(e.effectiveStatus())).toList();
    }
    public List<Audit> history(UUID actor) {
        authorize(actor);
        return jdbc.query("SELECT * FROM compass.admin_audit ORDER BY id DESC LIMIT 100", (r, n) ->
                new Audit(r.getLong("id"), r.getObject("actor_id", UUID.class), r.getString("action"),
                        r.getObject("subject_id", UUID.class), r.getLong("revision"), r.getTimestamp("created_at").toInstant()));
    }
    public void audit(UUID actor, String action, UUID subject, long revision) {
        jdbc.update("INSERT INTO compass.admin_audit(actor_id,action,subject_id,revision) VALUES (?,?,?,?)", actor, action, subject, revision);
    }
}
