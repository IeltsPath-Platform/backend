CREATE TABLE notification_preferences (
    recipient_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL CHECK (event_type IN (
        'community.post-commented.v1',
        'community.comment-replied.v1',
        'community.comment-mentioned.v1',
        'community.post-reacted.v1',
        'community.user-followed.v1'
    )),
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (recipient_id, event_type)
);
