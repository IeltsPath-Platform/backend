-- Game rooms and sessions reach clients over WebSocket and Redis; no consumer reads this outbox and no relay publishes
-- it. Dropping the table and its index removes a false signal about pending events.
DROP INDEX IF EXISTS idx_outbox_events_pending;
DROP TABLE IF EXISTS outbox_events;
