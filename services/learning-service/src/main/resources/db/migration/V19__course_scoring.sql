-- A topic is done once its test was taken, whatever the score; passed_at still marks 70% for display.
ALTER TABLE topic_progress ADD COLUMN tested_at TIMESTAMPTZ;
-- Percent (0-100) of the first topic test. It is the learner's mid-course score for the topic.
ALTER TABLE topic_progress ADD COLUMN first_test_percent DOUBLE PRECISION
    CHECK (first_test_percent IS NULL OR first_test_percent BETWEEN 0 AND 100);

-- Topics tested before this change: the first test they took counts, passed or not.
UPDATE topic_progress tp SET tested_at = first.consumed_at, first_test_percent = first.percent
FROM (SELECT DISTINCT ON (user_id, topic_id) user_id, topic_id, consumed_at, percent
      FROM topic_test_assignments WHERE consumed_at IS NOT NULL AND percent IS NOT NULL
      ORDER BY user_id, topic_id, consumed_at) first
WHERE first.user_id = tp.user_id AND first.topic_id = tp.topic_id;
UPDATE topic_progress SET tested_at = passed_at WHERE tested_at IS NULL AND passed_at IS NOT NULL AND has_topic_test;

-- Weights and thresholds as fractions of 1. A row with a course applies to that course; the row without one is the
-- default for every other course.
CREATE TABLE course_score_configs (
    id UUID PRIMARY KEY,
    course_id UUID UNIQUE,
    process_weight NUMERIC(4,3) NOT NULL CHECK (process_weight BETWEEN 0 AND 1),
    mid_weight NUMERIC(4,3) NOT NULL CHECK (mid_weight BETWEEN 0 AND 1),
    final_weight NUMERIC(4,3) NOT NULL CHECK (final_weight BETWEEN 0 AND 1),
    practice_share NUMERIC(4,3) NOT NULL CHECK (practice_share BETWEEN 0 AND 1),
    practice_test_share NUMERIC(4,3) NOT NULL CHECK (practice_test_share BETWEEN 0 AND 1),
    pass_threshold NUMERIC(4,3) NOT NULL CHECK (pass_threshold > 0 AND pass_threshold <= 1),
    mid_remediation_below NUMERIC(4,3) NOT NULL CHECK (mid_remediation_below BETWEEN 0 AND 1),
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (process_weight + mid_weight + final_weight = 1),
    CHECK (practice_share + practice_test_share = 1)
);
CREATE UNIQUE INDEX uq_course_score_configs_default ON course_score_configs ((TRUE)) WHERE course_id IS NULL;
-- Process 20%, mid-course 30%, final 50%. Process is Practice 0.64 and milestone practice tests 0.36, which keeps the
-- 45:25 ratio of the board now that homework is gone.
INSERT INTO course_score_configs (id, course_id, process_weight, mid_weight, final_weight, practice_share,
                                  practice_test_share, pass_threshold, mid_remediation_below)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 0.200, 0.300, 0.500, 0.640, 0.360, 0.700, 0.600);

-- Supplementary work asked for after a weak first topic test (MID_COURSE) or an unpassed course (POST_COURSE). It stays
-- open while any of its reviews is PENDING.
CREATE TABLE remediation_flags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    topic_id UUID,
    kind VARCHAR(12) NOT NULL CHECK (kind IN ('MID_COURSE', 'POST_COURSE')),
    source_attempt_id UUID NOT NULL,
    review_ids UUID[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source_attempt_id, kind)
);
CREATE INDEX idx_remediation_flags_user_course ON remediation_flags (user_id, course_id);

-- The decision on a course after a course test. PASSED is final; REMEDIATION turns into a retest once review_ids has no
-- PENDING review left.
CREATE TABLE course_outcomes (
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('PASSED', 'REMEDIATION')),
    total NUMERIC(5,4) NOT NULL,
    components JSONB NOT NULL,
    final_attempt_id UUID NOT NULL,
    top_error_codes VARCHAR(60)[] NOT NULL DEFAULT '{}',
    review_ids UUID[] NOT NULL DEFAULT '{}',
    decided_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, course_id)
);
