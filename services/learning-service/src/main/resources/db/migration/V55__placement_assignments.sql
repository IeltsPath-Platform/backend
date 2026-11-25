CREATE TABLE placement_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    package_id UUID NOT NULL,
    package_version_id UUID NOT NULL,
    pool VARCHAR(10) NOT NULL CHECK (pool IN ('BAND_0_3', 'BAND_3_5', 'BAND_5_9')),
    assigned_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    attempt_id UUID
);

CREATE INDEX idx_placement_assignments_user_assigned
    ON placement_assignments (user_id, assigned_at DESC);
CREATE INDEX idx_placement_assignments_user_package
    ON placement_assignments (user_id, package_id);
CREATE UNIQUE INDEX uq_placement_assignments_attempt
    ON placement_assignments (attempt_id) WHERE attempt_id IS NOT NULL;
