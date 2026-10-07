DO $$
DECLARE
    attempt_type_constraint TEXT;
BEGIN
    SELECT conname INTO attempt_type_constraint
    FROM pg_constraint
    WHERE conrelid = 'assessment_attempts'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) ILIKE '%attempt_type%';

    IF attempt_type_constraint IS NULL THEN
        RAISE EXCEPTION 'Could not find the assessment_attempts attempt_type check constraint';
    END IF;

    EXECUTE format('ALTER TABLE assessment_attempts DROP CONSTRAINT %I', attempt_type_constraint);
END $$;

ALTER TABLE assessment_attempts
    ADD CONSTRAINT chk_assessment_attempts_type
    CHECK (attempt_type IN ('PLACEMENT', 'OFFICIAL_PRACTICE', 'MOCK', 'TOPIC_GATE', 'QUIZ', 'COURSE_GATE'));
