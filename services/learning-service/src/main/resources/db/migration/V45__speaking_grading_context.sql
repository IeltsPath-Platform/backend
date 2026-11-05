-- What the grading job needs from Content besides the rubric: the Speaking error types and knowledge points. The job
-- has no learner token and cannot call Content, so the learner's own request captures them when the submission is
-- completed, the same moment the rubric is captured, and the job reads them from here.

CREATE TABLE speaking_grading_contexts (
    submission_id UUID PRIMARY KEY REFERENCES speaking_submissions (id) ON DELETE CASCADE,
    context       JSONB       NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
