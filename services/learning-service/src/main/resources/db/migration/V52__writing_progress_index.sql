-- Progress reads a learner's graded essays newest first; the partial index covers only graded rows.
CREATE INDEX idx_lesson_writing_progress ON lesson_writing_submissions (user_id, graded_at DESC) WHERE status = 'GRADED';
