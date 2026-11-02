CREATE TABLE admin_grant_requests (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash    CHAR(64)     NOT NULL,
    subscription_id UUID         NOT NULL REFERENCES subscriptions(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
