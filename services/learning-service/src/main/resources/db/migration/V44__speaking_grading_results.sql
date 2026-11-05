ALTER TABLE speaking_submissions
    ADD COLUMN result JSONB,
    ADD COLUMN overall_band NUMERIC(2,1),
    ADD COLUMN debit_ledger_entry_id UUID,
    ADD COLUMN rubric_code VARCHAR(80),
    ADD COLUMN rubric_version INTEGER,
    ADD COLUMN prompt_checksum VARCHAR(64),
    ADD COLUMN prompt_version VARCHAR(20),
    ADD COLUMN rubric_snapshot JSONB,
    ADD COLUMN grading_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN grading_started_at TIMESTAMPTZ,
    ADD COLUMN graded_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_speaking_submission_band CHECK (overall_band IS NULL OR overall_band BETWEEN 0 AND 9),
    ADD CONSTRAINT chk_speaking_submission_rubric_snapshot CHECK (
        (rubric_code IS NULL AND rubric_version IS NULL AND prompt_checksum IS NULL)
        OR (rubric_code IS NOT NULL AND rubric_version IS NOT NULL AND rubric_version > 0
            AND prompt_checksum ~ '^[0-9a-f]{64}$')),
    ADD CONSTRAINT chk_speaking_submission_grade CHECK (
        status <> 'GRADED' OR (result IS NOT NULL AND debit_ledger_entry_id IS NOT NULL)),
    ADD CONSTRAINT chk_speaking_submission_pending_payment CHECK (
        status <> 'PAYMENT_PENDING' OR result IS NOT NULL);

CREATE INDEX idx_speaking_submissions_grading_queue
    ON speaking_submissions (queued_at, created_at)
    WHERE status IN ('QUEUED', 'FAILED_RETRYABLE', 'ANALYZING', 'GRADING');

DROP INDEX uq_speaking_submissions_one_active;
CREATE UNIQUE INDEX uq_speaking_submissions_one_active
    ON speaking_submissions (user_id)
    WHERE status IN ('UPLOADING', 'QUEUED', 'ANALYZING', 'GRADING', 'PAYMENT_PENDING', 'FAILED_RETRYABLE');

CREATE TABLE speaking_analyses (
    submission_id UUID NOT NULL REFERENCES speaking_submissions(id) ON DELETE CASCADE,
    question_version_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ANALYZED', 'FAILED_RETRYABLE')),
    result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (submission_id, question_version_id)
);

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe',
     'free_practice', 'error_trap', 'speaking_practice'));

ALTER TABLE speaking_recordings ADD COLUMN question_text TEXT NOT NULL DEFAULT '';
