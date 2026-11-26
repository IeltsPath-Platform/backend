CREATE TABLE deleted_users (
    user_id UUID PRIMARY KEY,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_deletion_jobs (
    event_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'DONE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_learning_user_deletion_jobs_pending
    ON user_deletion_jobs (created_at)
    WHERE status = 'PENDING';
