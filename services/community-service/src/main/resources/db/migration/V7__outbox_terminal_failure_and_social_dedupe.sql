ALTER TABLE outbox_events ADD COLUMN failed_at TIMESTAMPTZ;

DROP INDEX idx_outbox_due_unpublished;
CREATE INDEX idx_outbox_due_unpublished
    ON outbox_events(next_attempt_at, created_at, id)
    WHERE published_at IS NULL AND failed_at IS NULL;

CREATE INDEX idx_outbox_social_dedupe
    ON outbox_events(event_type, (payload->>'actorId'), (payload->>'recipientId'), created_at DESC)
    WHERE event_type IN ('community.post-reacted.v1', 'community.user-followed.v1');
