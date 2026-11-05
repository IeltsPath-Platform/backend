-- Work in progress of a standalone human review, so an examiner who closes the page or loses the connection keeps
-- what was typed. draft_payload has the shape of the final review (band, criteria, feedback, annotations) but any part may
-- be missing; the strict checks run only when the review is completed. Learners never read it: only the final columns of
-- human_review_submissions are returned to them.
-- Reviews of mock and course tests do not use these columns: their draft is a DRAFT result version.
ALTER TABLE human_reviews ADD COLUMN draft_payload JSONB;
ALTER TABLE human_reviews ADD COLUMN draft_saved_at TIMESTAMPTZ;

ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_object
    CHECK (draft_payload IS NULL OR jsonb_typeof(draft_payload) = 'object');
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_size
    CHECK (draft_payload IS NULL OR octet_length(draft_payload::text) <= 204800);
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_saved_at
    CHECK ((draft_payload IS NULL) = (draft_saved_at IS NULL));

-- A completed review keeps no draft: the statement that sets COMPLETED must clear the draft in the same UPDATE.
ALTER TABLE human_reviews ADD CONSTRAINT chk_human_reviews_draft_open
    CHECK (draft_payload IS NULL OR status <> 'COMPLETED');
