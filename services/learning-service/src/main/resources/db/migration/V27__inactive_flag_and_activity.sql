-- A learner who has studied, is still on a course, and has not answered anything for a few days gets an INACTIVE flag.
-- The flag closes (DISMISSED) on the learner's next answer.
ALTER TABLE learner_flags DROP CONSTRAINT learner_flags_kind_check;
ALTER TABLE learner_flags ADD CONSTRAINT learner_flags_kind_check CHECK (kind IN
    ('TOO_FAST_HIGH_SCORE', 'BURST_SUBMISSIONS', 'DUPLICATE_ESSAY', 'INACTIVE'));

-- Daily, weekly and monthly activity and retention scan the event log by time, not by learner.
CREATE INDEX idx_qae_occurred_at ON question_attempt_events (occurred_at);
