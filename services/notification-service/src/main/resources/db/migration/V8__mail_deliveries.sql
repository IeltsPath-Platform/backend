CREATE TABLE mail_deliveries (
    event_id UUID PRIMARY KEY,
    sent_at TIMESTAMPTZ NOT NULL
);
