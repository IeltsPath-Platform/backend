CREATE TABLE practice_test_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_attempt_id UUID,
    consumed_at TIMESTAMPTZ,
    percent DOUBLE PRECISION CHECK (percent BETWEEN 0 AND 100),
    UNIQUE (user_id, package_id),
    CHECK ((consumed_attempt_id IS NULL AND consumed_at IS NULL AND percent IS NULL)
        OR (consumed_attempt_id IS NOT NULL AND consumed_at IS NOT NULL AND percent IS NOT NULL))
);
CREATE INDEX idx_practice_test_assignments_course ON practice_test_assignments(user_id, course_id);
CREATE INDEX idx_practice_test_assignments_open ON practice_test_assignments(user_id, package_version_id)
    WHERE consumed_attempt_id IS NULL;
