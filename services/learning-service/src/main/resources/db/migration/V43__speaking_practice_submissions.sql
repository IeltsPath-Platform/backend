-- AI-graded Speaking practice: one submission per attempt at a Speaking test (or at some of its parts), with one row per
-- question to record. A submission is created before any file is stored; the recordings then go to object storage under
-- practice/{userId}/{date}_{submissionId}/ and are listed here by object key.

CREATE TABLE speaking_submissions (
    id                 uuid PRIMARY KEY,
    user_id            uuid         NOT NULL,
    request_id         uuid         NOT NULL,
    package_version_id uuid         NOT NULL,
    scope              varchar(10)  NOT NULL,
    status             varchar(20)  NOT NULL,
    point_cost         integer      NOT NULL,
    consent_at         timestamptz  NOT NULL,
    failure_code       varchar(60),
    created_at         timestamptz  NOT NULL,
    updated_at         timestamptz  NOT NULL,
    queued_at          timestamptz,
    CONSTRAINT uq_speaking_submissions_request UNIQUE (request_id),
    CONSTRAINT chk_speaking_submissions_scope CHECK (scope IN ('PART', 'FULL')),
    CONSTRAINT chk_speaking_submissions_status CHECK (status IN (
        'UPLOADING', 'QUEUED', 'ANALYZING', 'GRADING', 'PAYMENT_PENDING', 'GRADED',
        'FAILED_RETRYABLE', 'NEEDS_EXAMINER', 'ABANDONED')),
    CONSTRAINT chk_speaking_submissions_cost CHECK (point_cost > 0)
);

-- A learner has at most one submission waiting to be graded: a balance that is charged only after grading must not be
-- spent several times over by submissions sent at once.
CREATE UNIQUE INDEX uq_speaking_submissions_one_active
    ON speaking_submissions (user_id)
    WHERE status IN ('UPLOADING', 'QUEUED', 'ANALYZING', 'GRADING', 'PAYMENT_PENDING');

CREATE INDEX idx_speaking_submissions_user_created ON speaking_submissions (user_id, created_at DESC);

CREATE TABLE speaking_recordings (
    submission_id       uuid         NOT NULL REFERENCES speaking_submissions (id) ON DELETE CASCADE,
    question_version_id uuid         NOT NULL,
    section_id          uuid         NOT NULL,
    part_no             integer      NOT NULL,
    question_no         integer      NOT NULL,
    attempt             integer      NOT NULL DEFAULT 0,
    object_key          varchar(300),
    content_type        varchar(60),
    size_bytes          bigint,
    duration_ms         integer,
    uploaded_at         timestamptz,
    PRIMARY KEY (submission_id, question_version_id),
    CONSTRAINT chk_speaking_recordings_position CHECK (part_no > 0 AND question_no > 0),
    CONSTRAINT chk_speaking_recordings_attempt CHECK (attempt >= 0),
    CONSTRAINT chk_speaking_recordings_uploaded CHECK (
        (object_key IS NULL AND uploaded_at IS NULL)
        OR (object_key IS NOT NULL AND uploaded_at IS NOT NULL AND content_type IS NOT NULL AND size_bytes > 0))
);
