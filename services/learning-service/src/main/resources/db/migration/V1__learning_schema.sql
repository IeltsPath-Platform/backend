CREATE TABLE knowledge_point_catalog (
    kp_id UUID PRIMARY KEY,
    topic_id UUID NOT NULL,
    has_practice_set BOOLEAN NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_knowledge_point_catalog_topic ON knowledge_point_catalog (topic_id);

CREATE TABLE topic_progress (
    user_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    sequence_order INTEGER,
    passed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, topic_id)
);

CREATE TABLE lesson_progress (
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    lesson_sort_order INTEGER NOT NULL,
    knowledge_point_ids UUID[] NOT NULL DEFAULT '{}',
    passed_block_ids TEXT[] NOT NULL DEFAULT '{}',
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, lesson_id)
);
CREATE INDEX idx_lesson_progress_knowledge_points ON lesson_progress USING GIN (knowledge_point_ids);
CREATE INDEX idx_lesson_progress_user_topic ON lesson_progress (user_id, topic_id, lesson_sort_order);

CREATE TABLE lesson_exercise_submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    block_id UUID NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    answers JSONB NOT NULL,
    block_passed BOOLEAN NOT NULL,
    response JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_lesson_exercise_submissions_block
    ON lesson_exercise_submissions (user_id, lesson_id, block_id);

CREATE TABLE review_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    knowledge_point_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'DONE', 'SKIPPED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    done_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_review_items_pending_kp
    ON review_items (user_id, knowledge_point_id) WHERE status = 'PENDING';

CREATE TABLE review_sets (
    id UUID PRIMARY KEY,
    review_item_id UUID NOT NULL REFERENCES review_items (id),
    user_id UUID NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    passed BOOLEAN,
    request_id UUID UNIQUE,
    response JSONB
);
CREATE UNIQUE INDEX uq_review_sets_open
    ON review_sets (review_item_id) WHERE submitted_at IS NULL;
CREATE INDEX idx_review_sets_user_package ON review_sets (user_id, package_id);

CREATE TABLE topic_test_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_attempt_id UUID,
    consumed_at TIMESTAMPTZ,
    percent DOUBLE PRECISION
);
CREATE UNIQUE INDEX uq_topic_test_assignments_open
    ON topic_test_assignments (user_id, topic_id) WHERE consumed_at IS NULL;
CREATE INDEX idx_topic_test_assignments_user_version
    ON topic_test_assignments (user_id, package_version_id, assigned_at);

CREATE TABLE kp_evidence (
    id UUID PRIMARY KEY,
    -- Insertion order; mastery weights recent answers, and created_at is shared by one transaction.
    ordinal BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id UUID NOT NULL,
    kp_id UUID NOT NULL,
    correct BOOLEAN NOT NULL,
    source VARCHAR(30) NOT NULL CHECK (source IN ('lesson_exercise', 'review_set', 'assessment')),
    source_reference_id UUID NOT NULL,
    attempt_id UUID,
    result_version INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, source_reference_id)
);
CREATE INDEX idx_kp_evidence_mastery ON kp_evidence (user_id, kp_id, ordinal);
CREATE INDEX idx_kp_evidence_assessment ON kp_evidence (user_id, attempt_id, result_version)
    WHERE source = 'assessment';

CREATE TABLE assessment_result_versions (
    user_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    result_version INTEGER NOT NULL CHECK (result_version >= 1),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, attempt_id)
);
