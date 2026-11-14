CREATE TABLE compass.user_account (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE CHECK (email = lower(btrim(email))),
    password_hash varchar(255) NOT NULL,
    role varchar(10) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
    verified_at timestamptz,
    credential_version bigint NOT NULL DEFAULT 0 CHECK (credential_version >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE compass.identity_token (
    token_hash char(64) PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    purpose varchar(10) NOT NULL CHECK (purpose IN ('VERIFY','RESET')),
    expires_at timestamptz NOT NULL,
    UNIQUE (user_id, purpose)
);
CREATE INDEX identity_token_expiry ON compass.identity_token(expires_at);
CREATE TABLE compass.auth_rate (
    bucket_hash char(64) PRIMARY KEY,
    window_start timestamptz NOT NULL,
    attempts integer NOT NULL CHECK (attempts > 0)
);
CREATE TABLE compass.saved_profile (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL UNIQUE REFERENCES compass.user_account ON DELETE CASCADE,
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    profile_json jsonb NOT NULL CHECK (jsonb_typeof(profile_json) = 'object'),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE compass.recommendation_snapshot (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    profile_json jsonb NOT NULL CHECK (jsonb_typeof(profile_json) = 'object'),
    result_json jsonb NOT NULL CHECK (jsonb_typeof(result_json) = 'object'),
    catalog_version uuid NOT NULL REFERENCES compass.catalog_version,
    engine_version varchar(100) NOT NULL,
    policy_version varchar(100) NOT NULL,
    calculated_at timestamptz NOT NULL,
    saved_at timestamptz NOT NULL,
    UNIQUE (id, owner_id)
);
CREATE INDEX recommendation_snapshot_owner ON compass.recommendation_snapshot(owner_id, saved_at DESC);
CREATE TABLE compass.saved_stack (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    snapshot_id uuid NOT NULL,
    bundle_id varchar(200) NOT NULL,
    created_at timestamptz NOT NULL,
    FOREIGN KEY (snapshot_id, owner_id) REFERENCES compass.recommendation_snapshot(id, owner_id) ON DELETE CASCADE,
    UNIQUE (snapshot_id, bundle_id)
);
CREATE INDEX saved_stack_owner ON compass.saved_stack(owner_id);
