ALTER TABLE review_items DROP CONSTRAINT review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC', 'WRITING_CRITERION'));
