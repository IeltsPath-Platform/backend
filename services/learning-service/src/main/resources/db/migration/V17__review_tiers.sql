-- An optional review (picked from the review basket, or raised by a free practice test) never blocks a lesson,
-- Practice or topic test. Every existing review is blocking.
ALTER TABLE review_items ADD COLUMN blocking BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE review_items DROP CONSTRAINT IF EXISTS review_items_trigger_kind_check;
ALTER TABLE review_items ADD CONSTRAINT review_items_trigger_kind_check CHECK (trigger_kind IN
    ('PRACTICE', 'ASSESSMENT', 'CONSOLIDATE', 'MAINTAIN', 'FREE_PRACTICE_TEST', 'DIAGNOSTIC'));
