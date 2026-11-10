CREATE INDEX ix_notification_push_expired
    ON notification_push_deliveries(expires_at, id)
    WHERE status IN ('PENDING','RETRY','IN_FLIGHT');

CREATE INDEX ix_notification_push_exhausted_due
    ON notification_push_deliveries(attempt_count, expires_at, id)
    WHERE status IN ('PENDING','RETRY');
