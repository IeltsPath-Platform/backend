ALTER TABLE knowledge_point_catalog
    ADD COLUMN parent_kp_id UUID,
    ADD COLUMN error_code VARCHAR(60);

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe',
     'free_practice', 'error_trap'));

ALTER TABLE review_items DROP CONSTRAINT review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC',
     'WRITING_CRITERION', 'ERROR_PATTERN'));
