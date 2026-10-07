-- An essay of a practice set is graded like a lesson essay and stored with them. A practice essay belongs to its
-- attempt and question instead of a lesson block.
ALTER TABLE lesson_writing_submissions ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'LESSON_BLOCK'
    CHECK (source IN ('LESSON_BLOCK', 'PRACTICE_ITEM'));
-- No foreign key: the use case checks that the attempt is the learner's and still open when an essay is sent.
ALTER TABLE lesson_writing_submissions ADD COLUMN practice_attempt_id UUID;
ALTER TABLE lesson_writing_submissions ALTER COLUMN block_id DROP NOT NULL;
ALTER TABLE lesson_writing_submissions ADD CONSTRAINT chk_lesson_writing_source CHECK (
    (source = 'LESSON_BLOCK' AND block_id IS NOT NULL AND practice_attempt_id IS NULL)
    OR (source = 'PRACTICE_ITEM' AND block_id IS NULL AND practice_attempt_id IS NOT NULL));
-- At most one essay of a practice question is being graded at a time for a learner.
CREATE UNIQUE INDEX uq_practice_writing_in_flight
    ON lesson_writing_submissions (user_id, practice_attempt_id, question_version_id)
    WHERE status = 'GRADING' AND source = 'PRACTICE_ITEM';
CREATE INDEX idx_practice_writing_attempt
    ON lesson_writing_submissions (practice_attempt_id, question_version_id, submitted_at DESC)
    WHERE practice_attempt_id IS NOT NULL;
