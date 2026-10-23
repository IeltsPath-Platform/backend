-- Stable reference for spreadsheet imports and AI drafts. Null for questions created before it existed.
-- Unique only where present, so many questions can have no reference.
ALTER TABLE questions ADD COLUMN import_ref VARCHAR(100);
CREATE UNIQUE INDEX uq_questions_import_ref ON questions (import_ref) WHERE import_ref IS NOT NULL;
