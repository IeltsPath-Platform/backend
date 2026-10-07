ALTER TABLE topic_progress ADD COLUMN course_id UUID;

CREATE TABLE course_progress (
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    passed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, course_id)
);

CREATE TABLE course_test_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    consumed_attempt_id UUID,
    consumed_at TIMESTAMPTZ,
    percent NUMERIC(5,2),
    CHECK ((consumed_attempt_id IS NULL) = (consumed_at IS NULL))
);

CREATE UNIQUE INDEX ux_course_test_assignments_open
    ON course_test_assignments (user_id, course_id) WHERE consumed_at IS NULL;
CREATE INDEX ix_course_test_assignments_rotation
    ON course_test_assignments (user_id, course_id, consumed_at);
CREATE INDEX ix_course_test_assignments_attempt
    ON course_test_assignments (user_id, package_version_id, assigned_at);
