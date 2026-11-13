-- Phones that receive FCM push. The app chooses the installation id; an installation belongs to whoever signed in last.
CREATE TABLE push_devices (
    installation_id VARCHAR(200) PRIMARY KEY,
    user_id UUID NOT NULL,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    token VARCHAR(512) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_push_devices_user ON push_devices(user_id);

-- One push of a stored notification to one installation; retries update the row instead of adding a notification.
CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    installation_id VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    provider_message_id VARCHAR(255),
    last_error VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    UNIQUE (notification_id, installation_id)
);
CREATE INDEX idx_notification_deliveries_due ON notification_deliveries(next_attempt_at) WHERE status = 'PENDING';
