CREATE INDEX idx_notifications_read_retention ON notifications(created_at, id) WHERE read_at IS NOT NULL;
CREATE INDEX idx_notifications_unread_retention ON notifications(created_at, id) WHERE read_at IS NULL;
