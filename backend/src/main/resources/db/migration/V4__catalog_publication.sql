-- P02 PUBLISHED means a sealed import. Public visibility requires explicit review and activation.
CREATE TABLE compass.catalog_review (
    version_id uuid PRIMARY KEY REFERENCES compass.catalog_version(id),
    content_hash text NOT NULL,
    reviewer text NOT NULL CHECK (length(trim(reviewer)) BETWEEN 1 AND 200),
    reviewed_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE compass.catalog_publication (
    revision bigint PRIMARY KEY CHECK (revision > 0),
    version_id uuid NOT NULL REFERENCES compass.catalog_review(version_id),
    previous_version_id uuid REFERENCES compass.catalog_review(version_id),
    action text NOT NULL CHECK (action IN ('PUBLISH', 'ROLLBACK')),
    actor text NOT NULL CHECK (length(trim(actor)) BETWEEN 1 AND 200),
    reason text NOT NULL CHECK (length(trim(reason)) BETWEEN 1 AND 2000),
    activated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (revision, version_id)
);
CREATE TABLE compass.catalog_active (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    version_id uuid,
    revision bigint NOT NULL DEFAULT 0,
    CHECK ((revision = 0 AND version_id IS NULL) OR (revision > 0 AND version_id IS NOT NULL)),
    FOREIGN KEY (revision, version_id) REFERENCES compass.catalog_publication(revision, version_id)
);
INSERT INTO compass.catalog_active(singleton) VALUES (true);
CREATE FUNCTION compass.guard_catalog_review() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'INSERT' THEN RAISE EXCEPTION 'Catalog review is append-only'; END IF;
    IF NOT EXISTS (SELECT 1 FROM compass.catalog_version WHERE id = NEW.version_id
        AND state = 'PUBLISHED' AND content_hash = NEW.content_hash) THEN
        RAISE EXCEPTION 'Review requires matching sealed content';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_review BEFORE INSERT OR UPDATE OR DELETE ON compass.catalog_review
    FOR EACH ROW EXECUTE FUNCTION compass.guard_catalog_review();
CREATE FUNCTION compass.guard_catalog_publication() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE current_revision bigint; current_version uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN RAISE EXCEPTION 'Publication history is append-only'; END IF;
    SELECT revision, version_id INTO current_revision, current_version FROM compass.catalog_active
        WHERE singleton FOR UPDATE;
    IF NEW.revision <> current_revision + 1 OR NEW.previous_version_id IS DISTINCT FROM current_version THEN
        RAISE EXCEPTION 'Publication must extend current revision';
    END IF;
    IF NEW.action = 'ROLLBACK' AND NOT EXISTS (SELECT 1 FROM compass.catalog_publication WHERE version_id = NEW.version_id) THEN
        RAISE EXCEPTION 'Rollback requires a previously activated snapshot';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_publication BEFORE INSERT OR UPDATE OR DELETE ON compass.catalog_publication
    FOR EACH ROW EXECUTE FUNCTION compass.guard_catalog_publication();
CREATE FUNCTION compass.guard_catalog_active() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'UPDATE' THEN RAISE EXCEPTION 'Active singleton cannot be removed or inserted'; END IF;
    IF NEW.revision <> OLD.revision + 1 THEN RAISE EXCEPTION 'Active revision must advance once'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_active BEFORE INSERT OR UPDATE OR DELETE ON compass.catalog_active
    FOR EACH ROW EXECUTE FUNCTION compass.guard_catalog_active();
CREATE FUNCTION compass.reject_publication_truncate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Publication state cannot be truncated'; END $$;
CREATE TRIGGER no_truncate_review BEFORE TRUNCATE ON compass.catalog_review FOR EACH STATEMENT EXECUTE FUNCTION compass.reject_publication_truncate();
CREATE TRIGGER no_truncate_publication BEFORE TRUNCATE ON compass.catalog_publication FOR EACH STATEMENT EXECUTE FUNCTION compass.reject_publication_truncate();
CREATE TRIGGER no_truncate_active BEFORE TRUNCATE ON compass.catalog_active FOR EACH STATEMENT EXECUTE FUNCTION compass.reject_publication_truncate();
