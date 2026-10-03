ALTER TABLE review_items ADD COLUMN stage VARCHAR(10) NOT NULL DEFAULT 'PRACTICE'
    CHECK (stage IN ('PRACTICE', 'THEORY'));
ALTER TABLE review_items ADD COLUMN theory_reason VARCHAR(30)
    CHECK (theory_reason IN ('SECOND_FAIL', 'LOW_SCORE', 'WRONG_IN_LESSON'));
ALTER TABLE review_items ADD COLUMN theory_completed_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE review_sets ADD COLUMN correct_count INTEGER;
ALTER TABLE review_sets ADD COLUMN total_count INTEGER;

CREATE TABLE review_theory_checks (
    id UUID PRIMARY KEY,
    review_item_id UUID NOT NULL REFERENCES review_items(id),
    user_id UUID NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    question_version_ids UUID[] NOT NULL,
    answers JSONB NOT NULL,
    correct_count INTEGER NOT NULL,
    total_count INTEGER NOT NULL,
    response JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_review_theory_checks_item ON review_theory_checks (review_item_id, submitted_at);

-- A pending review that already failed a set and has no open set reads the theory once before its next set.
UPDATE review_items r SET stage = 'THEORY', theory_reason = 'SECOND_FAIL'
WHERE r.status = 'PENDING'
  AND NOT EXISTS (SELECT 1 FROM review_sets s WHERE s.review_item_id = r.id AND s.submitted_at IS NULL)
  AND EXISTS (SELECT 1 FROM review_sets s WHERE s.review_item_id = r.id AND s.passed = FALSE);
