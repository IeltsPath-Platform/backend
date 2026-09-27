-- One short, tutor-written memory per learner, shared by all of their paths. Rebuilt in batches from messages whose id
-- is above last_message_id; deleting it clears the text and moves the mark past every existing message.
CREATE TABLE learner_memory (
    user_id UUID PRIMARY KEY,
    content TEXT NOT NULL DEFAULT '' CHECK (char_length(content) <= 2000),
    last_message_id BIGINT NOT NULL DEFAULT 0 CHECK (last_message_id >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL
);
