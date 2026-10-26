-- A notification can point at a grading job (the learner's examiner is late) instead of a community post or comment.
ALTER TABLE notifications ADD COLUMN grading_job_id UUID;
