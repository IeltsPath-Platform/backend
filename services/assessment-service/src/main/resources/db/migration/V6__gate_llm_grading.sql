-- Essays of topic and course tests are graded by the LLM in the background: the job keeps the band it returned and
-- when it started, so a job left PROCESSING by a crash can be queued again. LLM calls are limited per learner and day.
ALTER TABLE grading_jobs ADD COLUMN llm_band NUMERIC(2,1) CHECK (llm_band BETWEEN 0 AND 9);
ALTER TABLE grading_jobs ADD COLUMN processing_started_at TIMESTAMPTZ;
CREATE INDEX idx_grading_jobs_status_mode ON grading_jobs (status, grading_mode);

CREATE TABLE llm_usage_daily (
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    count INTEGER NOT NULL CHECK (count >= 0),
    PRIMARY KEY (user_id, usage_date)
);
