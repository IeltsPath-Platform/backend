ALTER TABLE learner_submissions ADD COLUMN recording_deleted_at TIMESTAMPTZ;

CREATE INDEX idx_learner_submissions_speaking_retention
    ON learner_submissions (skill, submitted_at)
    WHERE skill = 'SPEAKING' AND recording_deleted_at IS NULL;
