CREATE TABLE mail_daily_cap_reservations (
    user_id UUID NOT NULL,
    sent_date DATE NOT NULL,
    event_id UUID NOT NULL,
    template VARCHAR(64) NOT NULL,
    slot SMALLINT NOT NULL CHECK (slot BETWEEN 1 AND 3),
    reserved_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, sent_date, event_id, template),
    UNIQUE (user_id, sent_date, slot)
);
