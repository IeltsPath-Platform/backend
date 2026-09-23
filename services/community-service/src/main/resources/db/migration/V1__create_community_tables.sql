CREATE TABLE posts (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('GENERAL','QUESTION','DISCUSSION')),
    title VARCHAR(200),
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','HIDDEN','DELETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
    ,version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_posts_feed ON posts(status, created_at DESC, id DESC);
CREATE INDEX idx_posts_author ON posts(author_id, created_at DESC);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id),
    author_id UUID NOT NULL,
    parent_comment_id UUID REFERENCES comments(id),
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','HIDDEN','DELETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
    ,version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_comments_post_parent ON comments(post_id, parent_comment_id, created_at, id);
CREATE INDEX idx_comments_feed ON comments(post_id, status, created_at, id);
CREATE INDEX idx_comments_author ON comments(author_id, created_at DESC);

CREATE TABLE post_reactions (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type IN ('LIKE','LOVE')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(post_id, user_id, reaction_type)
);
CREATE INDEX idx_post_reactions_post_type ON post_reactions(post_id, reaction_type);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(published_at, created_at);
