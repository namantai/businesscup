CREATE TABLE compass.catalog_draft (
    id uuid PRIMARY KEY,
    author_id uuid REFERENCES compass.user_account ON DELETE SET NULL,
    manifest_text text NOT NULL CHECK (octet_length(manifest_text) <= 5000000),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    state varchar(12) NOT NULL DEFAULT 'DRAFT' CHECK (state IN ('DRAFT','VALIDATED','REVIEWED','PUBLISHED')),
    version_id uuid REFERENCES compass.catalog_version,
    reviewed_by uuid REFERENCES compass.user_account ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE compass.admin_audit (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id uuid REFERENCES compass.user_account ON DELETE SET NULL,
    action varchar(40) NOT NULL,
    subject_id uuid NOT NULL,
    revision bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
