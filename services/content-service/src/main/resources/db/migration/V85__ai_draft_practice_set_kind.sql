-- A model-written practice set is a third kind of AI draft beside questions and courses.
ALTER TABLE ai_drafts DROP CONSTRAINT IF EXISTS ai_drafts_kind_check;
ALTER TABLE ai_drafts ADD CONSTRAINT ai_drafts_kind_check CHECK (kind IN ('QUESTIONS', 'COURSE', 'PRACTICE_SET'));
