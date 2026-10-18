-- One row per question per submission: the per-question event log that mentors use to see time spent and error types.
-- Mastery evidence (kp_evidence) stays per knowledge point; this table is per question.
--
-- attempt_id is not in the original plan. It lets an assessment regrade replace its own events, as it already replaces
-- its evidence, without touching lesson, practice, review or writing events.
CREATE TABLE question_attempt_events (
    id UUID PRIMARY KEY,
    ordinal BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    knowledge_point_ids UUID[] NOT NULL DEFAULT '{}',
    correct BOOLEAN,
    score NUMERIC(8,2),
    max_score NUMERIC(8,2),
    duration_ms BIGINT CHECK (duration_ms IS NULL OR duration_ms BETWEEN 0 AND 7200000),
    error_code VARCHAR(60),
    source VARCHAR(30) NOT NULL CHECK (source IN
        ('lesson_exercise', 'practice_set', 'review_set', 'lesson_writing', 'assessment', 'diagnostic_probe')),
    source_reference_id UUID NOT NULL,
    attempt_id UUID,
    first_submission BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, source_reference_id, question_version_id)
);

CREATE INDEX idx_qae_user_time ON question_attempt_events (user_id, occurred_at);
CREATE INDEX idx_qae_question ON question_attempt_events (question_version_id);
CREATE INDEX idx_qae_user_kp ON question_attempt_events USING GIN (knowledge_point_ids);
CREATE INDEX idx_qae_user_attempt ON question_attempt_events (user_id, attempt_id) WHERE attempt_id IS NOT NULL;
