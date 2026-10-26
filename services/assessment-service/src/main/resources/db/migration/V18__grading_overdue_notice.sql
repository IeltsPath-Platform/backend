-- The learner is told once when the examiner they chose has not graded in time. The moment of the notice is kept on the
-- job so it is not repeated, and is cleared when the job is handed to another examiner (their deadline starts again).
ALTER TABLE grading_jobs ADD COLUMN overdue_notified_at TIMESTAMPTZ;

CREATE INDEX idx_grading_jobs_overdue_unnoticed ON grading_jobs (requested_at)
    WHERE grading_mode = 'HUMAN' AND status IN ('QUEUED', 'PROCESSING')
      AND requested_examiner_id IS NOT NULL AND overdue_notified_at IS NULL;
