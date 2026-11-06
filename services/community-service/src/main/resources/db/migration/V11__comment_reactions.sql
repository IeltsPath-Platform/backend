CREATE TABLE comment_reactions (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type IN ('LIKE', 'LOVE', 'HELPFUL', 'ACCURATE')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX idx_comment_reactions_comment_type ON comment_reactions (comment_id, reaction_type);
