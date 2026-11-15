-- The page "Lịch sử nộp bài" lists a learner's submissions newest first across lessons and sources. The existing indexes
-- of these two tables start with lesson or block, so they cannot serve a per-user timeline.
CREATE INDEX idx_lesson_writing_user_submitted ON lesson_writing_submissions (user_id, submitted_at DESC);
CREATE INDEX idx_practice_attempts_user_submitted ON practice_attempts (user_id, submitted_at DESC)
    WHERE submitted_at IS NOT NULL;
