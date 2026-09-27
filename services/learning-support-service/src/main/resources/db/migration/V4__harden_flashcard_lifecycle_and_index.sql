ALTER TABLE flashcards
    ADD COLUMN version bigint NOT NULL DEFAULT 0;

DROP INDEX IF EXISTS idx_flashcards_user_status_updated;

CREATE INDEX idx_flashcards_user_status_updated_id
    ON flashcards (user_id, status, updated_at DESC, id DESC);
