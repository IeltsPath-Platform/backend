-- The examiner is reminded once, 36 hours after being chosen, that a job will be late in 12 hours. The moment of the
-- reminder is kept on the job so it is not repeated, and is cleared when the job is handed to another examiner.
ALTER TABLE grading_jobs ADD COLUMN due_soon_notified_at TIMESTAMPTZ;

CREATE INDEX idx_grading_jobs_due_soon_unnoticed ON grading_jobs (requested_at)
    WHERE grading_mode = 'HUMAN' AND status IN ('QUEUED', 'PROCESSING')
      AND requested_examiner_id IS NOT NULL AND due_soon_notified_at IS NULL;
