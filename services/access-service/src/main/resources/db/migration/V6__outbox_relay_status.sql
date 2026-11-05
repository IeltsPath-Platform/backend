-- Outbox rows of types the relay does not publish are kept as NOT_RELAYED. Rows that were PENDING before this migration
-- were written before any relay existed, so they are kept as NOT_RELAYED too: no old notice is sent to learners.
ALTER TABLE outbox_events DROP CONSTRAINT outbox_events_status_check;
ALTER TABLE outbox_events ADD CONSTRAINT outbox_events_status_check
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED', 'NOT_RELAYED'));

UPDATE outbox_events SET status = 'NOT_RELAYED' WHERE status = 'PENDING';

ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(500);

CREATE INDEX idx_access_outbox_pending ON outbox_events(created_at) WHERE status = 'PENDING';
