-- One row per learner and knowledge point: the mastery score, its tier and when the point is due for review.
-- It is derived from kp_evidence and question_attempt_events and can be rebuilt from them.
CREATE TABLE kp_mastery_profiles (
    user_id UUID NOT NULL,
    knowledge_point_id UUID NOT NULL,
    mastery NUMERIC(4,3) NOT NULL CHECK (mastery BETWEEN 0 AND 1),
    tier VARCHAR(12) NOT NULL CHECK (tier IN ('PRIORITY', 'CONSOLIDATE', 'MAINTAIN')),
    evidence_count INTEGER NOT NULL CHECK (evidence_count >= 0),
    success_streak INTEGER NOT NULL DEFAULT 0 CHECK (success_streak >= 0),
    last_evidence_at TIMESTAMPTZ NOT NULL,
    next_review_at TIMESTAMPTZ,
    median_duration_ms BIGINT CHECK (median_duration_ms IS NULL OR median_duration_ms >= 0),
    top_error_codes VARCHAR(60)[] NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, knowledge_point_id)
);
CREATE INDEX idx_kp_mastery_due ON kp_mastery_profiles (user_id, next_review_at);

-- Admin overrides of the mastery rules. A key without a row falls back to the service configuration.
CREATE TABLE learning_rule_configs (
    key VARCHAR(60) PRIMARY KEY,
    value JSONB NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
