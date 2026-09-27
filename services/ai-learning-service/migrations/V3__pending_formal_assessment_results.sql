-- Finalized Assessment results that arrived before the learner's path for that goal existed.
-- The consumer parks them here and ACKs; the transaction that creates the path applies and
-- deletes them. Rows are kept until the path is created (no expiry yet).
CREATE TABLE pending_formal_assessment_results (
    event_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    learning_goal_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    result_version INTEGER NOT NULL CHECK (result_version > 0),
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pending_formal_results_goal
    ON pending_formal_assessment_results (user_id, learning_goal_id, attempt_id, result_version);
