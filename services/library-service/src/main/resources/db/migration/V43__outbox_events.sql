-- Transactional outbox for events Library publishes. The relay publishes PENDING rows and marks them PUBLISHED, or FAILED
-- once the attempts run out. The row id is the event id, so queuing the same event twice is a no-op.
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count    INT          NOT NULL DEFAULT 0,
    last_error     VARCHAR(500),
    correlation_id VARCHAR(100),
    trace_context  VARCHAR,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_library_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';
