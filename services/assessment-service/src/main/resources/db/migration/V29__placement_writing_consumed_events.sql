-- Placement Writing results already applied, so a redelivered result cannot take an essay back from the local grader.
CREATE TABLE consumed_placement_writing_events (
    event_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
