-- Seconds the learner actively spent in each section of a practice attempt, reported by the client when it submits.
ALTER TABLE attempt_sections ADD COLUMN time_spent_seconds INTEGER
    CHECK (time_spent_seconds IS NULL OR time_spent_seconds BETWEEN 0 AND 86400);
