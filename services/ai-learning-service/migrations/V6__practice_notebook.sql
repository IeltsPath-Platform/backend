-- Content metadata of each knowledge point in a path, copied with the bands when the path is built or refreshed,
-- so the tutor can write practice questions without a learner token. NULL skill means Content gave none.
CREATE TABLE mastery_path_knowledge_point_details (
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL,
    skill VARCHAR(50),
    description TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (path_id, knowledge_point_id)
);

-- Practice questions are never a mastery authority. Ownership is explicit and unanswered cards have no answer
-- timestamp, so API projections can omit the answer key until the learner submits an answer.
CREATE TABLE notebook_entries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    turn_id UUID REFERENCES turns(id) ON DELETE SET NULL,
    mastery_path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL,
    knowledge_point_name TEXT NOT NULL DEFAULT '',
    question_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('short', 'choice')),
    options_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    correct_answer TEXT NOT NULL,
    explanation TEXT NOT NULL DEFAULT '',
    difficulty VARCHAR(20) NOT NULL DEFAULT '',
    source VARCHAR(50) NOT NULL DEFAULT 'tutor_practice',
    user_answer TEXT NOT NULL DEFAULT '',
    result VARCHAR(20) NOT NULL DEFAULT '' CHECK (result IN ('', 'correct', 'incorrect')),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMPTZ,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notebook_entries_question UNIQUE (session_id, turn_id, question_id)
);
CREATE INDEX idx_notebook_entries_user_created ON notebook_entries (user_id, created_at DESC);
CREATE INDEX idx_notebook_entries_user_kp ON notebook_entries (user_id, knowledge_point_id);

CREATE TABLE practice_review_state (
    entry_id BIGINT PRIMARY KEY REFERENCES notebook_entries(id) ON DELETE CASCADE,
    is_mistake BOOLEAN NOT NULL DEFAULT TRUE,
    first_wrong_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    interval_days NUMERIC(8, 3) NOT NULL DEFAULT 1 CHECK (interval_days > 0),
    ease NUMERIC(4, 2) NOT NULL DEFAULT 2.5 CHECK (ease BETWEEN 1.3 AND 3.0),
    streak INTEGER NOT NULL DEFAULT 0 CHECK (streak >= 0),
    lapses INTEGER NOT NULL DEFAULT 0 CHECK (lapses >= 0),
    review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    last_review_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_practice_review_state_due ON practice_review_state (due_at) WHERE is_mistake;

CREATE TABLE practice_review_events (
    request_id UUID PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES notebook_entries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating VARCHAR(10) NOT NULL CHECK (rating IN ('again', 'hard', 'good', 'easy')),
    answer TEXT NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    outcome_json JSONB NOT NULL
);
CREATE INDEX idx_practice_review_events_entry ON practice_review_events (entry_id, reviewed_at DESC);
