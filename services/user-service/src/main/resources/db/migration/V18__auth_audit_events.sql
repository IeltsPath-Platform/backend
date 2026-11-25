CREATE TABLE auth_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    event_type VARCHAR(40) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    correlation_id VARCHAR(64),
    detail JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_audit_user_created ON auth_audit_events (user_id, created_at DESC);
CREATE INDEX idx_auth_audit_type_created ON auth_audit_events (event_type, created_at DESC);
