CREATE TABLE human_review_submissions (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    learner_id UUID NOT NULL,
    source_submission_id UUID NOT NULL UNIQUE,
    source VARCHAR(20) NOT NULL CHECK (source IN ('LESSON','PRACTICE')),
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1','TASK_2')),
    prompt_snapshot JSONB NOT NULL,
    essay_text TEXT NOT NULL,
    rubric_snapshot JSONB NOT NULL,
    band NUMERIC(2,1) CHECK (band BETWEEN 0 AND 9 AND band * 2 = trunc(band * 2)),
    criteria JSONB,
    error_codes VARCHAR(60)[],
    feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CHECK ((band IS NULL AND criteria IS NULL AND error_codes IS NULL AND feedback IS NULL AND completed_at IS NULL)
        OR (band IS NOT NULL AND criteria IS NOT NULL AND error_codes IS NOT NULL AND feedback IS NOT NULL
            AND completed_at IS NOT NULL))
);

ALTER TABLE grading_jobs ALTER COLUMN submission_id DROP NOT NULL;
ALTER TABLE grading_jobs ADD COLUMN human_review_submission_id UUID UNIQUE
    REFERENCES human_review_submissions(id) ON DELETE RESTRICT;
ALTER TABLE grading_jobs ADD CONSTRAINT grading_jobs_one_submission CHECK
    (num_nonnulls(submission_id, human_review_submission_id) = 1);

CREATE INDEX idx_human_review_submissions_learner ON human_review_submissions(learner_id, created_at DESC);
