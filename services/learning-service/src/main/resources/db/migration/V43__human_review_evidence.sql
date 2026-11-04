-- An examiner's standalone Writing review becomes mastery evidence ('human_review') and study points. The outcome row
-- records that one review was applied, so an event delivered twice is applied once, and keeps the study points the
-- learner reads back on the review.
ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe',
     'free_practice', 'error_trap', 'human_review'));

CREATE TABLE human_review_outcomes (
    review_submission_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    submission_id UUID NOT NULL UNIQUE REFERENCES lesson_writing_submissions (id),
    study_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
