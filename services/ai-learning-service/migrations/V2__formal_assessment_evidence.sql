-- Evidence projection (DATABASE_V5 7.5). Rebuilt from mastery_paths.state_json inside every
-- aggregate commit; it is an index, never the mastery authority.
CREATE TABLE IF NOT EXISTS mastery_learning_evidence (
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    ordinal BIGINT NOT NULL,
    knowledge_point_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(50) NOT NULL,
    source_reference_id UUID,
    assessment_type VARCHAR(30) NOT NULL,
    result VARCHAR(20) NOT NULL,
    quality NUMERIC(5,4) CHECK (quality IS NULL OR quality BETWEEN 0 AND 1),
    hints_used INTEGER NOT NULL DEFAULT 0,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    confidence NUMERIC(5,4) CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    response_time_seconds NUMERIC,
    session_id UUID,
    turn_id UUID,
    evidence_json JSONB NOT NULL,
    PRIMARY KEY (path_id, ordinal)
);

CREATE INDEX IF NOT EXISTS idx_mastery_evidence_kp_time
    ON mastery_learning_evidence (path_id, knowledge_point_id, occurred_at DESC);

-- One formal outcome (result, version, item, knowledge point) can exist at most once per path.
-- Replaces the non-unique (path_id, source, source_reference_id) index documented in DATABASE_V5.
DROP INDEX IF EXISTS idx_mastery_evidence_source_reference;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mastery_evidence_source_reference
    ON mastery_learning_evidence (path_id, source, source_reference_id)
    WHERE source_reference_id IS NOT NULL;

-- Latest applied Assessment result version per attempt. Read and advanced under the
-- mastery_paths row lock, so redelivered, late or regraded versions are decided atomically
-- with the aggregate mutation. The WHERE-guarded upsert refuses a non-increasing version.
CREATE TABLE IF NOT EXISTS formal_assessment_result_versions (
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    attempt_id UUID NOT NULL,
    result_id UUID NOT NULL,
    result_version INTEGER NOT NULL CHECK (result_version > 0),
    event_id UUID NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (path_id, attempt_id)
);
