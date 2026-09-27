-- Existing notes have no source. Source values are validated by the NoteSourceType enum.
ALTER TABLE notes ADD COLUMN source_type varchar(50);
ALTER TABLE notes ADD COLUMN source_reference_id uuid;
ALTER TABLE notes ADD CONSTRAINT chk_notes_source_pair
    CHECK ((source_type IS NULL) = (source_reference_id IS NULL));
CREATE INDEX idx_notes_user_source
    ON notes (user_id, source_type, source_reference_id, updated_at DESC)
    WHERE source_type IS NOT NULL;
