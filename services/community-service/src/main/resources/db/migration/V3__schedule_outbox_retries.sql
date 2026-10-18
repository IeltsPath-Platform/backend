ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_due_unpublished
    ON outbox_events(next_attempt_at, created_at, id)
    WHERE published_at IS NULL;
