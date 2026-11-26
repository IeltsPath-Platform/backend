CREATE INDEX idx_access_outbox_published
    ON outbox_events (published_at)
    WHERE status = 'PUBLISHED';
