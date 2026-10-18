-- Free practice: a set chosen for one knowledge point outside any lesson. Its attempt has no lesson and origin FREE;
-- lesson attempts keep their lesson and origin LESSON.
ALTER TABLE practice_attempts ALTER COLUMN lesson_id DROP NOT NULL;
ALTER TABLE practice_attempts ADD COLUMN origin VARCHAR(10) NOT NULL DEFAULT 'LESSON'
    CHECK (origin IN ('LESSON', 'FREE'));
ALTER TABLE practice_attempts ADD COLUMN knowledge_point_id UUID;
ALTER TABLE practice_attempts ADD CONSTRAINT practice_attempts_origin_shape CHECK (
    (origin = 'FREE') = (lesson_id IS NULL)
    AND (origin = 'LESSON' OR knowledge_point_id IS NOT NULL));

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe',
     'free_practice'));

ALTER TABLE question_attempt_events DROP CONSTRAINT question_attempt_events_source_check;
ALTER TABLE question_attempt_events ADD CONSTRAINT question_attempt_events_source_check CHECK (source IN
    ('lesson_exercise', 'practice_set', 'review_set', 'lesson_writing', 'assessment', 'diagnostic_probe',
     'free_practice'));
