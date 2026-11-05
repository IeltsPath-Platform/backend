CREATE TABLE learner_course_unlocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    skill VARCHAR(20),
    point_cost INTEGER NOT NULL CHECK (point_cost >= 0),
    request_id UUID NOT NULL UNIQUE,
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'CANCELLED')),
    ledger_entry_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    activated_at TIMESTAMPTZ,
    CONSTRAINT learner_course_unlock_skill_check
        CHECK (skill IS NULL OR skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING'))
);

CREATE UNIQUE INDEX learner_course_unlocks_one_owned_target
    ON learner_course_unlocks (user_id, course_id, COALESCE(skill, 'ALL'))
    WHERE status <> 'CANCELLED';

CREATE INDEX learner_course_unlocks_pending_by_user
    ON learner_course_unlocks (user_id, created_at)
    WHERE status = 'PENDING';

CREATE INDEX learner_course_unlocks_active_by_course
    ON learner_course_unlocks (user_id, course_id)
    WHERE status = 'ACTIVE';
