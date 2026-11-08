CREATE TABLE notification_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    installation_id UUID NOT NULL,
    provider VARCHAR(16) NOT NULL CHECK (provider = 'FCM'),
    platform VARCHAR(16) NOT NULL CHECK (platform = 'ANDROID'),
    token TEXT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    app_version VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    registration_version BIGINT NOT NULL DEFAULT 1 CHECK (registration_version > 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_registered_at TIMESTAMPTZ NOT NULL,
    disabled_at TIMESTAMPTZ,
    CONSTRAINT uq_notification_device_installation UNIQUE (provider, installation_id)
);

CREATE UNIQUE INDEX uq_notification_device_active_token
    ON notification_devices(provider, token_hash) WHERE active;
CREATE INDEX ix_notification_devices_user_active
    ON notification_devices(user_id, active, id);
