ALTER TABLE topic_progress ADD COLUMN skill VARCHAR(20);
ALTER TABLE topic_progress ADD COLUMN has_topic_test BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE knowledge_point_catalog ADD COLUMN skill VARCHAR(20);
ALTER TABLE review_items ADD COLUMN skill VARCHAR(20);
CREATE INDEX idx_review_items_user_skill_pending ON review_items (user_id, skill) WHERE status = 'PENDING';

-- Demo Writing W1/W2 now own the essay blocks formerly attached to Reading L3/L4.
UPDATE lesson_writing_submissions SET lesson_id = '25000000-0000-4000-8000-030000000001'
WHERE block_id = '22000000-0000-4000-8000-040000000002'
  AND lesson_id = '20000000-0000-4000-8000-030000000003';
UPDATE lesson_writing_submissions SET lesson_id = '25000000-0000-4000-8000-030000000002'
WHERE block_id = '21000000-0000-4000-8000-040000000002'
  AND lesson_id = '20000000-0000-4000-8000-030000000004';
UPDATE review_items SET lesson_id = '25000000-0000-4000-8000-030000000001'
WHERE knowledge_point_id = '22000000-0000-4000-8000-020000000006'
  AND lesson_id = '20000000-0000-4000-8000-030000000003';
UPDATE review_items SET lesson_id = '25000000-0000-4000-8000-030000000002'
WHERE knowledge_point_id = '21000000-0000-4000-8000-020000000007'
  AND lesson_id = '20000000-0000-4000-8000-030000000004';
