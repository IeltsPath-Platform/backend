CREATE TABLE notification_push_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    device_id UUID NOT NULL REFERENCES notification_devices(id) ON DELETE RESTRICT,
    registration_version BIGINT NOT NULL CHECK (registration_version > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','IN_FLIGHT','RETRY','SENT','PARKED','CANCELLED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    claim_id UUID,
    lease_until TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    provider_message_id VARCHAR(256),
    last_error_code VARCHAR(64),
    cancellation_reason VARCHAR(32),
    correlation_id VARCHAR(128),
    trace_context JSONB,
    CONSTRAINT uq_notification_push_delivery_binding UNIQUE (notification_id, device_id, registration_version),
    CONSTRAINT ck_notification_push_lease CHECK ((status = 'IN_FLIGHT') = (claim_id IS NOT NULL AND lease_until IS NOT NULL))
);

CREATE INDEX ix_notification_push_due
    ON notification_push_deliveries(next_attempt_at, id) WHERE status IN ('PENDING','RETRY');
CREATE INDEX ix_notification_push_lease
    ON notification_push_deliveries(lease_until, id) WHERE status = 'IN_FLIGHT';
CREATE INDEX ix_notification_push_recipient ON notification_push_deliveries(recipient_id, created_at);
