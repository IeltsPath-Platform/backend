CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE assessment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    attempt_type VARCHAR(40) NOT NULL CHECK (attempt_type IN ('PLACEMENT','OFFICIAL_PRACTICE','MOCK','TOPIC_GATE','QUIZ')),
    mode VARCHAR(30) NOT NULL CHECK (mode IN ('STANDARD','TIMED')),
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('WEB','MOBILE','API')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS','SUBMITTED','EXPIRED','CANCELLED')),
    started_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attempt_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE RESTRICT,
    content_section_id UUID NOT NULL,
    sort_order INT NOT NULL CHECK (sort_order >= 0),
    section_snapshot JSONB NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    UNIQUE (attempt_id, sort_order)
);

CREATE TABLE attempt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_section_id UUID NOT NULL REFERENCES attempt_sections(id) ON DELETE RESTRICT,
    question_version_id UUID NOT NULL,
    sort_order INT NOT NULL CHECK (sort_order >= 0),
    question_snapshot JSONB NOT NULL,
    answer_snapshot JSONB,
    knowledge_snapshot JSONB,
    UNIQUE (attempt_section_id, sort_order)
);

CREATE TABLE attempt_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_item_id UUID NOT NULL REFERENCES attempt_items(id) ON DELETE RESTRICT,
    response_payload JSONB NOT NULL,
    schema_version INT NOT NULL DEFAULT 1 CHECK (schema_version > 0),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0),
    saved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    UNIQUE (attempt_item_id)
);

CREATE TABLE learner_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    attempt_item_id UUID REFERENCES attempt_items(id) ON DELETE RESTRICT,
    prompt_snapshot JSONB NOT NULL,
    skill VARCHAR(20) NOT NULL CHECK (skill IN ('WRITING','SPEAKING')),
    text_payload TEXT,
    audio_reference VARCHAR(500),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED')),
    submission_key VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, submission_key),
    CHECK ((skill = 'WRITING' AND text_payload IS NOT NULL AND audio_reference IS NULL)
        OR (skill = 'SPEAKING' AND audio_reference IS NOT NULL AND text_payload IS NULL))
);

CREATE TABLE assessment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE RESTRICT,
    result_version INT NOT NULL CHECK (result_version > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','PROCESSING','COMPLETED','FAILED')),
    overall_band NUMERIC(3,1) CHECK (overall_band >= 0),
    completed_at TIMESTAMPTZ,
    UNIQUE (attempt_id, result_version)
);

CREATE TABLE skill_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL REFERENCES assessment_results(id) ON DELETE RESTRICT,
    skill VARCHAR(20) NOT NULL CHECK (skill IN ('LISTENING','READING','WRITING','SPEAKING')),
    raw_score NUMERIC(8,2) CHECK (raw_score >= 0),
    band NUMERIC(3,1) CHECK (band >= 0),
    grading_source VARCHAR(20) NOT NULL CHECK (grading_source IN ('AUTO','AI','HUMAN')),
    feedback_revision_id UUID,
    UNIQUE (result_id, skill)
);

CREATE TABLE item_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL REFERENCES assessment_results(id) ON DELETE RESTRICT,
    attempt_item_id UUID NOT NULL REFERENCES attempt_items(id) ON DELETE RESTRICT,
    score NUMERIC(8,2) NOT NULL CHECK (score >= 0),
    is_correct BOOLEAN,
    duration_milliseconds BIGINT CHECK (duration_milliseconds >= 0),
    feedback_snapshot JSONB NOT NULL,
    UNIQUE (result_id, attempt_item_id)
);

CREATE TABLE error_analysis_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_result_id UUID NOT NULL REFERENCES item_results(id) ON DELETE RESTRICT,
    knowledge_point_id UUID,
    error_type VARCHAR(50) NOT NULL,
    explanation TEXT
);

CREATE TABLE video_practice_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    video_id UUID NOT NULL,
    segment_id UUID NOT NULL,
    practice_type VARCHAR(30) NOT NULL CHECK (practice_type IN ('DICTATION','SHADOWING')),
    reference_text_snapshot TEXT NOT NULL,
    response_text TEXT,
    audio_reference VARCHAR(500),
    score NUMERIC(5,2) CHECK (score >= 0),
    result_payload JSONB,
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS','COMPLETED','FAILED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE grading_point_costs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill VARCHAR(20) NOT NULL CHECK (skill IN ('WRITING','SPEAKING')),
    grading_mode VARCHAR(20) NOT NULL CHECK (grading_mode IN ('AI','HUMAN')),
    point_cost INT NOT NULL CHECK (point_cost >= 0),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE TABLE grading_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES learner_submissions(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL,
    skill VARCHAR(20) NOT NULL CHECK (skill IN ('WRITING','SPEAKING')),
    grading_mode VARCHAR(20) NOT NULL CHECK (grading_mode IN ('AI','HUMAN')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED','CANCELLED')),
    point_cost_snapshot INT CHECK (point_cost_snapshot IS NULL OR point_cost_snapshot >= 0),
    point_ledger_entry_id UUID,
    premium_subscription_id UUID,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CHECK ((grading_mode = 'AI' AND point_cost_snapshot IS NOT NULL) OR (grading_mode = 'HUMAN' AND point_cost_snapshot IS NULL))
);

CREATE TABLE human_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grading_job_id UUID NOT NULL UNIQUE REFERENCES grading_jobs(id) ON DELETE RESTRICT,
    grader_user_id UUID,
    status VARCHAR(30) NOT NULL CHECK (status IN ('QUEUED','ASSIGNED','IN_REVIEW','COMPLETED')),
    assigned_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    reviewer_note TEXT
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    retry_count INT NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    last_error TEXT
);

CREATE INDEX idx_assessment_attempts_user_status_created ON assessment_attempts(user_id, status, created_at DESC);
CREATE INDEX idx_assessment_attempts_package_version ON assessment_attempts(package_version_id);
CREATE INDEX idx_attempt_sections_attempt_order ON attempt_sections(attempt_id, sort_order);
CREATE INDEX idx_attempt_items_section_order ON attempt_items(attempt_section_id, sort_order);
CREATE INDEX idx_learner_submissions_user_status ON learner_submissions(user_id, status, submitted_at DESC);
CREATE INDEX idx_assessment_results_attempt_version ON assessment_results(attempt_id, result_version DESC);
CREATE INDEX idx_item_results_attempt_item ON item_results(attempt_item_id);
CREATE INDEX idx_error_analysis_knowledge_point ON error_analysis_items(knowledge_point_id);
CREATE INDEX idx_grading_jobs_status_created ON grading_jobs(status, created_at);
CREATE INDEX idx_video_practice_user_created ON video_practice_attempts(user_id, created_at DESC);
CREATE INDEX idx_outbox_pending ON outbox_events(published_at, created_at);
