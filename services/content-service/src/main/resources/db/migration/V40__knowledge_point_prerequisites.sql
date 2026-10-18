CREATE TABLE knowledge_point_prerequisites (
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id) ON DELETE CASCADE,
    prerequisite_id UUID NOT NULL REFERENCES knowledge_points(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (knowledge_point_id, prerequisite_id),
    CONSTRAINT chk_kp_prerequisite_not_self CHECK (knowledge_point_id <> prerequisite_id)
);

CREATE INDEX idx_kp_prerequisites_prerequisite
    ON knowledge_point_prerequisites (prerequisite_id);
