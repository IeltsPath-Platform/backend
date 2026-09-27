-- Tutor study/review runtime: a learner's sessions on their active path, the turns of each session, and the
-- learner/tutor messages. Simpler than DATABASE_V5 7.7-7.10: one API instance serves each turn inside one HTTP
-- request, so there is no worker ownership, fencing token or per-event log. Questions and grading results live in
-- mastery_interactions; tool calls are not stored as messages.

CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL DEFAULT 'New session',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ
);

CREATE INDEX idx_sessions_user_updated
    ON sessions (user_id, updated_at DESC)
    WHERE archived_at IS NULL;

CREATE TABLE turns (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('running', 'completed', 'failed')),
    failure_code VARCHAR(100) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ
);

-- At most one running turn per session: a second concurrent turn is rejected, not queued.
CREATE UNIQUE INDEX uq_turns_one_running_per_session
    ON turns (session_id)
    WHERE status = 'running';

CREATE TABLE messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    turn_id UUID REFERENCES turns(id) ON DELETE SET NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_messages_session
    ON messages (session_id, id);
