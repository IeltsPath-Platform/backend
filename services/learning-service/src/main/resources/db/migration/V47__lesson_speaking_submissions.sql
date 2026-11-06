-- A Speaking answer given inside a lesson's practice set. It is the same submission as a free Speaking practice, with the
-- lesson and the practice attempt it belongs to; free practice leaves both empty. Only a submission with an attempt can
-- reach mastery, when that attempt is submitted.
ALTER TABLE speaking_submissions
    ADD COLUMN lesson_id UUID,
    ADD COLUMN practice_attempt_id UUID,
    ADD CONSTRAINT chk_speaking_submission_course_context CHECK ((lesson_id IS NULL) = (practice_attempt_id IS NULL));

CREATE INDEX idx_speaking_submissions_attempt
    ON speaking_submissions (practice_attempt_id, created_at DESC)
    WHERE practice_attempt_id IS NOT NULL;

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe',
     'free_practice', 'error_trap', 'speaking_practice', 'human_review', 'lesson_speaking'));
