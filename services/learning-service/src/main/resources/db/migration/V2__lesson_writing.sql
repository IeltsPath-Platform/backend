-- One graded essay per row. The grade is stored before payment so a resend never calls the LLM twice, and it is
-- shown to the learner only once the row is GRADED (points debited).
CREATE TABLE lesson_writing_submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    block_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    knowledge_point_ids UUID[] NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    essay_text TEXT NOT NULL,
    word_count INTEGER NOT NULL CHECK (word_count >= 0),
    -- stem, task, minWords, passBand, chartFacts, sampleAnswer, images: what the essay was graded against.
    prompt_snapshot JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GRADING', 'GRADED', 'FAILED', 'PAYMENT_PENDING')),
    point_cost INTEGER NOT NULL CHECK (point_cost > 0),
    debit_ledger_entry_id UUID,
    failure_code VARCHAR(50),
    result JSONB,
    overall_band NUMERIC(2,1) CHECK (overall_band BETWEEN 0 AND 9),
    passed BOOLEAN,
    grading_started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    graded_at TIMESTAMPTZ,
    CHECK (status <> 'GRADED' OR (result IS NOT NULL AND overall_band IS NOT NULL
                                  AND passed IS NOT NULL AND debit_ledger_entry_id IS NOT NULL)),
    CHECK (status <> 'PAYMENT_PENDING' OR (result IS NOT NULL AND passed IS NOT NULL))
);
-- At most one essay of a block is being graded at a time for a learner.
CREATE UNIQUE INDEX uq_lesson_writing_in_flight ON lesson_writing_submissions (user_id, block_id)
    WHERE status = 'GRADING';
CREATE INDEX idx_lesson_writing_user_lesson
    ON lesson_writing_submissions (user_id, lesson_id, block_id, submitted_at DESC);

-- LLM calls per learner, day (in the configured quota time zone) and kind of call.
CREATE TABLE llm_daily_usage (
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    kind VARCHAR(30) NOT NULL,
    count INTEGER NOT NULL CHECK (count >= 0),
    PRIMARY KEY (user_id, usage_date, kind)
);

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check
    CHECK (source IN ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing'));
