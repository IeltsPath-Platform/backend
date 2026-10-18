ALTER TABLE flashcards
    ADD COLUMN leitner_box smallint NOT NULL DEFAULT 1,
    ADD COLUMN next_review_at timestamptz,
    ADD COLUMN last_reviewed_at timestamptz,
    ADD COLUMN review_count integer NOT NULL DEFAULT 0,
    ADD COLUMN last_review_request_id uuid,
    ADD CONSTRAINT chk_flashcards_leitner_box CHECK (leitner_box BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_flashcards_review_count CHECK (review_count >= 0);

CREATE INDEX idx_flashcards_user_status_due
    ON flashcards (user_id, status, next_review_at);
