CREATE TABLE placement_speaking_jobs (
    event_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'GRADED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(80),
    correlation_id VARCHAR(100),
    trace_context VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_placement_speaking_jobs_claim
    ON placement_speaking_jobs (next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE TABLE placement_speaking_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(100),
    trace_context VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000)
);

CREATE INDEX idx_placement_speaking_outbox_unpublished
    ON placement_speaking_outbox (created_at)
    WHERE published_at IS NULL;
