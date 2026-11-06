-- A recorded answer judged weak on a Speaking criterion earns an optional review of that criterion's knowledge point,
-- as a weak Writing criterion does; the review's trigger names the criterion of a recording.
ALTER TABLE review_items DROP CONSTRAINT review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC',
     'WRITING_CRITERION', 'ERROR_PATTERN', 'SPEAKING_CRITERION'));
