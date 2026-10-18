-- A learner asked a Mentor (Examiner) to review a graded lesson or Practice essay. One request per essay: the credit is
-- spent once. Assessment keeps the review itself; Learning only remembers its id.
CREATE TABLE human_review_requests (
    submission_id UUID PRIMARY KEY REFERENCES lesson_writing_submissions (id),
    user_id UUID NOT NULL,
    review_submission_id UUID NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_human_review_requests_user ON human_review_requests (user_id, requested_at DESC);
