CREATE TABLE course_study_activity (
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    started_at TIMESTAMPTZ,
    last_studied_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, course_id),
    CHECK (started_at IS NULL OR last_studied_at >= started_at)
);
CREATE INDEX ix_course_study_activity_recent ON course_study_activity (user_id, last_studied_at DESC, course_id);
