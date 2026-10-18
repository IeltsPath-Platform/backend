CREATE TABLE examiner_qa_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL UNIQUE REFERENCES learner_submissions(id) ON DELETE RESTRICT,
    examiner_id UUID NOT NULL,
    examiner_band NUMERIC(2,1) NOT NULL CHECK (examiner_band BETWEEN 0 AND 9),
    reviewer_id UUID NOT NULL,
    reviewer_band NUMERIC(2,1) NOT NULL CHECK (reviewer_band BETWEEN 0 AND 9),
    reviewer_criteria JSONB NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (examiner_band * 2 = trunc(examiner_band * 2)),
    CHECK (reviewer_band * 2 = trunc(reviewer_band * 2)),
    CHECK (examiner_id <> reviewer_id)
);

CREATE INDEX idx_examiner_qa_reviews_examiner ON examiner_qa_reviews(examiner_id, created_at DESC);
