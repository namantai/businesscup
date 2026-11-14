ALTER TABLE compass.saved_stack ADD UNIQUE (id, owner_id);
CREATE TABLE compass.watched_stack (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    stack_id uuid NOT NULL UNIQUE,
    start_revision bigint NOT NULL,
    active boolean NOT NULL DEFAULT true,
    email_opt_in boolean NOT NULL DEFAULT false,
    last_recomputed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (stack_id,owner_id) REFERENCES compass.saved_stack(id,owner_id) ON DELETE CASCADE,
    UNIQUE(id,owner_id)
);
CREATE TABLE compass.monitor_job (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    watch_id uuid NOT NULL REFERENCES compass.watched_stack ON DELETE CASCADE,
    publication_revision bigint NOT NULL REFERENCES compass.catalog_publication,
    state varchar(10) NOT NULL DEFAULT 'READY' CHECK (state IN ('READY','RUNNING','DONE','FAILED')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
    lease_token uuid,
    lease_until timestamptz,
    available_at timestamptz NOT NULL DEFAULT now(),
    recomputed boolean NOT NULL DEFAULT false,
    UNIQUE(watch_id,publication_revision)
);
CREATE INDEX monitor_job_pending ON compass.monitor_job(available_at,id) WHERE state IN ('READY','RUNNING');
CREATE TABLE compass.change_notice (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    watch_id uuid NOT NULL,
    publication_revision bigint NOT NULL REFERENCES compass.catalog_publication,
    payload jsonb NOT NULL,
    read_at timestamptz,
    applied_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY(watch_id,owner_id) REFERENCES compass.watched_stack(id,owner_id) ON DELETE CASCADE,
    UNIQUE(watch_id,publication_revision)
);
CREATE INDEX change_notice_owner ON compass.change_notice(owner_id,created_at DESC);
-- This is a local outbox, deliberately without any network sender.
CREATE TABLE compass.notification_outbox (
    notice_id uuid PRIMARY KEY REFERENCES compass.change_notice ON DELETE CASCADE,
    state varchar(10) NOT NULL DEFAULT 'LOCAL_ONLY' CHECK (state IN ('LOCAL_ONLY','CANCELLED')),
    created_at timestamptz NOT NULL DEFAULT now()
);
