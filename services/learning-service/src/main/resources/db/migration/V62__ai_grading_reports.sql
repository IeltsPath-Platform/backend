CREATE TABLE ai_grading_reports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    submission_type VARCHAR(20) NOT NULL CHECK (submission_type IN ('WRITING', 'SPEAKING')),
    reason VARCHAR(1000) NOT NULL CHECK (length(btrim(reason)) BETWEEN 1 AND 1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'REVIEWED', 'DISMISSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_grading_reports_user_submission UNIQUE (user_id, submission_id)
);

CREATE INDEX idx_ai_grading_reports_status_created ON ai_grading_reports (status, created_at DESC, id DESC);
