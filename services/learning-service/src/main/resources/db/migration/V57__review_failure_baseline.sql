ALTER TABLE review_items ADD COLUMN failures_counted_from TIMESTAMPTZ;
ALTER TABLE review_items ADD COLUMN unblocked_at TIMESTAMPTZ;

ALTER TABLE review_items DROP CONSTRAINT review_items_theory_reason_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_theory_reason_check
    CHECK (theory_reason IN ('SECOND_FAIL', 'LOW_SCORE', 'WRONG_IN_LESSON', 'REPEATED_FAIL'));

ALTER TABLE review_items DROP CONSTRAINT review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC',
     'WRITING_CRITERION', 'ERROR_PATTERN', 'SPEAKING_CRITERION', 'PRIORITY', 'DECAYED'));
