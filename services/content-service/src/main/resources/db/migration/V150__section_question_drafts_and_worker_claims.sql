ALTER TABLE ai_drafts
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN claim_token UUID;

ALTER TABLE ai_drafts DROP CONSTRAINT IF EXISTS ai_drafts_kind_check;
ALTER TABLE ai_drafts ADD CONSTRAINT ai_drafts_kind_check
    CHECK (kind IN ('QUESTIONS', 'COURSE', 'PRACTICE_SET', 'SECTION_QUESTIONS'));

ALTER TABLE ai_drafts DROP CONSTRAINT IF EXISTS ai_drafts_status_check;
ALTER TABLE ai_drafts ADD CONSTRAINT ai_drafts_status_check
    CHECK (status IN ('QUEUED', 'GENERATING', 'READY', 'FAILED', 'APPROVED', 'REJECTED', 'EXPIRED'));

CREATE UNIQUE INDEX uq_ai_drafts_open_section
    ON ai_drafts (requested_by, (request ->> 'sectionId'))
    WHERE kind = 'SECTION_QUESTIONS' AND status IN ('QUEUED', 'GENERATING', 'READY');
