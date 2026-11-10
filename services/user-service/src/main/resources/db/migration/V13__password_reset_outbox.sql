CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    correlation_id VARCHAR(100),
    trace_context TEXT,
    CONSTRAINT chk_user_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_user_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_user_outbox_pending ON outbox_events (created_at, id) WHERE status = 'PENDING';
CREATE INDEX idx_user_outbox_failed ON outbox_events (created_at, id) WHERE status = 'FAILED';
CREATE INDEX idx_account_action_tokens_user_purpose_created
    ON account_action_tokens (user_id, purpose, created_at DESC);
