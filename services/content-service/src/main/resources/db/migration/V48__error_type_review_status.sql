-- Every error type seeded so far is a draft until an IELTS expert reviews it. Learners must not see a code or fix hint
-- whose status is still DRAFT.
ALTER TABLE error_types ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (review_status IN ('DRAFT', 'APPROVED'));
