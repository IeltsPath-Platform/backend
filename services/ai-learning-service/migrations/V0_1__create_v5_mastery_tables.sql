-- DATABASE_V5 7.1, 7.3 and 7.4: the DeepTutor Mastery Path aggregate, its interactions and its
-- internal event log. Later migrations (V1 goal uniqueness, V2 formal evidence) build on these.
-- FKs to sessions/turns are omitted because those tables do not exist in this database yet.

CREATE TABLE mastery_paths (
    path_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    learning_goal_id UUID,
    state_json JSONB NOT NULL,
    revision BIGINT NOT NULL CHECK (revision >= 0),
    owner_session_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_mastery_paths_user_updated
    ON mastery_paths (user_id, updated_at DESC);

CREATE INDEX idx_mastery_paths_learning_goal
    ON mastery_paths (learning_goal_id);

-- Status values are the lowercase strings DeepTutor writes, not the uppercase names in DATABASE_V5.
CREATE TABLE mastery_interactions (
    interaction_id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('registered', 'awaiting_input', 'answered', 'graded', 'abandoned')),
    question_json JSONB NOT NULL,
    session_id UUID,
    turn_id UUID,
    user_answer TEXT NOT NULL DEFAULT '',
    result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_mastery_interactions_path
    ON mastery_interactions (path_id, created_at DESC);

-- Same invariant as upstream: at most one active interaction per path.
CREATE UNIQUE INDEX uq_mastery_one_active_interaction
    ON mastery_interactions (path_id)
    WHERE status IN ('registered', 'awaiting_input', 'answered');

CREATE TABLE mastery_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    revision BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    session_id UUID,
    turn_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_mastery_events_path_revision
    ON mastery_events (path_id, revision, id);
