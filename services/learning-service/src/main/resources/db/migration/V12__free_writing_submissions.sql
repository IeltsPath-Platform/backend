-- A free writing essay is graded like a lesson or practice essay and stored with them. It answers a writing prompt
-- chosen from the library, so it belongs to no lesson, lesson block or practice attempt.
ALTER TABLE lesson_writing_submissions DROP CONSTRAINT lesson_writing_submissions_source_check;
ALTER TABLE lesson_writing_submissions ADD CONSTRAINT lesson_writing_submissions_source_check
    CHECK (source IN ('LESSON_BLOCK', 'PRACTICE_ITEM', 'FREE_WRITING'));

ALTER TABLE lesson_writing_submissions ALTER COLUMN lesson_id DROP NOT NULL;

-- Lesson and practice essays still need their lesson; a free essay has none of the three owners.
ALTER TABLE lesson_writing_submissions DROP CONSTRAINT chk_lesson_writing_source;
ALTER TABLE lesson_writing_submissions ADD CONSTRAINT chk_lesson_writing_source CHECK (
    (source = 'LESSON_BLOCK' AND lesson_id IS NOT NULL AND block_id IS NOT NULL AND practice_attempt_id IS NULL)
    OR (source = 'PRACTICE_ITEM' AND lesson_id IS NOT NULL AND block_id IS NULL AND practice_attempt_id IS NOT NULL)
    OR (source = 'FREE_WRITING' AND lesson_id IS NULL AND block_id IS NULL AND practice_attempt_id IS NULL));

-- At most one essay of a writing prompt is being graded at a time for a learner.
CREATE UNIQUE INDEX uq_free_writing_in_flight
    ON lesson_writing_submissions (user_id, question_version_id)
    WHERE status = 'GRADING' AND source = 'FREE_WRITING';
