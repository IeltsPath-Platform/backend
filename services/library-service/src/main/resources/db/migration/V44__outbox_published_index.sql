CREATE INDEX idx_library_outbox_published
    ON outbox_events (published_at)
    WHERE status = 'PUBLISHED';
