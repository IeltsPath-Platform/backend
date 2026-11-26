CREATE TABLE placement_speaking_grades (
    attempt_id UUID PRIMARY KEY REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'GRADED', 'FAILED')),
    deadline_at TIMESTAMPTZ NOT NULL,
    band NUMERIC(3,1),
    feedback JSONB,
    rubric_code VARCHAR(100),
    rubric_version INTEGER,
    failure_code VARCHAR(80),
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((status = 'GRADED' AND band IS NOT NULL) OR status <> 'GRADED')
);

CREATE TABLE consumed_speaking_grading_events (
    event_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL UNIQUE,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_placement_speaking_timeout
    ON placement_speaking_grades (deadline_at)
    WHERE status = 'PENDING';
