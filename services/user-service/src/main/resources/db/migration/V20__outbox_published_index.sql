CREATE INDEX idx_user_outbox_published
    ON outbox_events (published_at)
    WHERE status = 'PUBLISHED';
