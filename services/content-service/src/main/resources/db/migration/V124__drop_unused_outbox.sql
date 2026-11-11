-- Nothing in this service writes to outbox_events: no use case appends to it and no relay reads it. Dropping the
-- table and its index removes a false signal about pending events. Other content tables are untouched.
DROP INDEX IF EXISTS idx_outbox_events_published_created;
DROP TABLE IF EXISTS outbox_events;
