-- How long a learner should plan for a lesson. NULL means the author has not set one, and clients show nothing.
ALTER TABLE lessons
    ADD COLUMN estimated_minutes INT,
    ADD CONSTRAINT chk_lessons_estimated_minutes CHECK (estimated_minutes IS NULL OR estimated_minutes > 0);

-- Durations for the lessons seeded so far.
UPDATE lessons SET estimated_minutes = 10 WHERE estimated_minutes IS NULL;
