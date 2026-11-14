CREATE TABLE compass.analytics_consent (
    owner_id uuid PRIMARY KEY REFERENCES compass.user_account ON DELETE CASCADE,
    enabled boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Daily presence, not raw clickstream or revenue. No free-form event properties.
CREATE TABLE compass.product_event (
    owner_id uuid NOT NULL REFERENCES compass.user_account ON DELETE CASCADE,
    event_name varchar(30) NOT NULL CHECK (event_name IN ('RESULT_SAVED','CHANGE_NOTICE_VIEWED','REVISIT')),
    event_day date NOT NULL,
    PRIMARY KEY (owner_id,event_name,event_day)
);
CREATE INDEX product_event_retention ON compass.product_event(event_day);
