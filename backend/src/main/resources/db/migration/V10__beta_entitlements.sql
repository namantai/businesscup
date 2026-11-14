-- Limits are enforced while holding the owner row lock, including concurrent grants.
ALTER TABLE compass.saved_profile DROP CONSTRAINT saved_profile_owner_id_key;
CREATE INDEX saved_profile_owner ON compass.saved_profile(owner_id);
CREATE TABLE compass.beta_grant (
    owner_id uuid PRIMARY KEY REFERENCES compass.user_account ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    revision bigint NOT NULL CHECK (revision > 0)
);
CREATE TABLE compass.beta_grant_audit (
    request_id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    actor_id uuid REFERENCES compass.user_account ON DELETE SET NULL,
    action varchar(10) NOT NULL CHECK (action IN ('GRANT','REVOKE')),
    days integer NOT NULL CHECK (days BETWEEN 0 AND 90),
    expected_revision bigint NOT NULL CHECK (expected_revision >= 0),
    result_revision bigint NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX beta_grant_audit_owner ON compass.beta_grant_audit(owner_id,created_at);
