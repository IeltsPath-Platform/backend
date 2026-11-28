-- Placement essays graded with the essay grader of course and free practice. Result events share the outbox of the
-- placement Speaking results.
CREATE TABLE placement_writing_jobs (
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
    result JSONB,
    correlation_id VARCHAR(100),
    trace_context VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_placement_writing_jobs_claim
    ON placement_writing_jobs (next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');
