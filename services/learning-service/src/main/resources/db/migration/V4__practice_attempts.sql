CREATE TABLE practice_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    skill VARCHAR(20) NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    request_id UUID UNIQUE,
    correct_count INTEGER,
    total_count INTEGER,
    passed BOOLEAN,
    counted_as_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    response JSONB,
    CHECK ((submitted_at IS NULL) = (request_id IS NULL))
);
CREATE UNIQUE INDEX uq_practice_attempt_open ON practice_attempts (user_id, package_id) WHERE submitted_at IS NULL;
CREATE INDEX idx_practice_attempts_user_lesson ON practice_attempts (user_id, lesson_id, started_at DESC);

ALTER TABLE review_items ADD COLUMN trigger_kind VARCHAR(20) CHECK (trigger_kind IN ('PRACTICE', 'ASSESSMENT'));
ALTER TABLE review_items ADD COLUMN source_attempt_id UUID REFERENCES practice_attempts(id);
CREATE INDEX idx_review_items_user_lesson_trigger ON review_items (user_id, lesson_id, trigger_kind);

CREATE TABLE lesson_practice_passes (
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL
        CHECK (reason IN ('FIRST_SUBMISSION', 'REVIEW_FINISHED', 'ALL_SETS_ATTEMPTED', 'NO_PRACTICE')),
    passed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, lesson_id)
);

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check
    CHECK (source IN ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set'));
