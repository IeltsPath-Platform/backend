-- The learner chooses the examiner of every human grading job; there is no shared pool. requested_examiner_id is that
-- choice and requested_at is when it was made (the 48 hour deadline counts from it). Both are NULL while the job waits
-- for the learner to choose: no examiner sees such a job. examiner_changes counts how often the learner replaced the
-- examiner after the deadline passed.
ALTER TABLE grading_jobs ADD COLUMN requested_examiner_id UUID;
ALTER TABLE grading_jobs ADD COLUMN requested_at TIMESTAMPTZ;
ALTER TABLE grading_jobs ADD COLUMN examiner_changes INT NOT NULL DEFAULT 0 CHECK (examiner_changes BETWEEN 0 AND 2);
ALTER TABLE grading_jobs ADD CONSTRAINT chk_grading_jobs_requested_pair
    CHECK ((requested_examiner_id IS NULL) = (requested_at IS NULL));

-- A job an examiner already took keeps that examiner as the chosen one. Jobs nobody took wait for the learner's choice.
-- QA sample jobs are reviewed by staff, not chosen by learners, so they are left alone.
UPDATE grading_jobs SET requested_examiner_id = assigned_examiner_id, requested_at = COALESCE(claimed_at, created_at)
WHERE grading_mode = 'HUMAN' AND assigned_examiner_id IS NOT NULL AND COALESCE(reason, '') <> 'QA_SAMPLE';

-- The queue of one examiner: their open jobs, oldest first.
CREATE INDEX idx_grading_jobs_requested_examiner ON grading_jobs (requested_examiner_id, status, requested_at)
    WHERE grading_mode = 'HUMAN';
