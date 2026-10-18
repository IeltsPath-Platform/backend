CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,
    recipient_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    post_id UUID,
    comment_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);
CREATE INDEX idx_notifications_owner_created ON notifications(recipient_id, created_at DESC, id DESC);
CREATE INDEX idx_notifications_owner_unread ON notifications(recipient_id) WHERE read_at IS NULL;
