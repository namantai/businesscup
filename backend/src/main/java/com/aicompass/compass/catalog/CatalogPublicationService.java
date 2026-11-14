package com.aicompass.compass.catalog;

import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogPublicationService {
    private final JdbcTemplate jdbc;
    private final CatalogPolicy policy;

    public CatalogPublicationService(JdbcTemplate jdbc, CatalogPolicy policy) {
        this.jdbc = jdbc;
        this.policy = policy;
    }

    public record Active(@io.swagger.v3.oas.annotations.media.Schema(nullable = true) UUID versionId, long revision) {}

    public Active current() {
        return jdbc.queryForObject("SELECT version_id, revision FROM compass.catalog_active WHERE singleton",
                (rs, row) -> new Active(rs.getObject(1, UUID.class), rs.getLong(2)));
    }

    @Transactional
    public void review(UUID version, String expectedHash, String reviewer) {
        text(reviewer, 200);
        var rows = jdbc.queryForList("SELECT content_hash, dataset_kind FROM compass.catalog_version WHERE id = ? AND state = 'PUBLISHED'", version);
        if (rows.isEmpty()) throw new IllegalArgumentException("Sealed snapshot not found");
        if (!Objects.equals(expectedHash, rows.getFirst().get("content_hash"))) throw new IllegalArgumentException("Content hash mismatch");
        if (!policy.allowDemo() && !"REVIEWED".equals(rows.getFirst().get("dataset_kind"))) throw new IllegalArgumentException("Demo disabled");
        jdbc.update("INSERT INTO compass.catalog_review(version_id, content_hash, reviewer) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                version, expectedHash, reviewer);
    }

    @Transactional
    public Active activate(UUID version, long expectedRevision, boolean rollback, String actor, String reason) {
        text(actor, 200);
        text(reason, 2000);
        Active previous = jdbc.queryForObject("SELECT version_id, revision FROM compass.catalog_active WHERE singleton FOR UPDATE",
                (rs, row) -> new Active(rs.getObject(1, UUID.class), rs.getLong(2)));
        // Revision, rather than only UUID, also prevents stale writes after an A -> B -> A rollback.
        if (previous.revision() != expectedRevision) throw new IllegalStateException("Active revision changed; inspect current state before retrying");
        if (version.equals(previous.versionId())) throw new IllegalArgumentException("Snapshot is already active");
        Long reviewed = jdbc.queryForObject("""
                SELECT count(*) FROM compass.catalog_review r JOIN compass.catalog_version v ON v.id = r.version_id
                WHERE r.version_id = ? AND (v.dataset_kind = 'REVIEWED' OR ?)
                """, Long.class, version, policy.allowDemo());
        if (reviewed != 1) throw new IllegalArgumentException("Reviewed snapshot not found");
        long revision = previous.revision() + 1;
        jdbc.update("""
                INSERT INTO compass.catalog_publication(revision, version_id, previous_version_id, action, actor, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """, revision, version, previous.versionId(), rollback ? "ROLLBACK" : "PUBLISH", actor, reason);
        jdbc.update("UPDATE compass.catalog_active SET version_id = ?, revision = ? WHERE singleton", version, revision);
        return new Active(version, revision);
    }

    private static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Invalid audit text");
    }
}
