-- Login attempts of existing accounts, newest first per user. Attempts for unknown emails are not stored.
CREATE TABLE login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(30),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_login_history_failure_reason CHECK (
        (success AND failure_reason IS NULL)
        OR (NOT success AND failure_reason IS NOT NULL AND failure_reason IN ('BAD_PASSWORD', 'ACCOUNT_LOCKED', 'ACCOUNT_INACTIVE', 'ACCOUNT_UNVERIFIED'))
    )
);

CREATE INDEX idx_login_history_user_created ON login_history (user_id, created_at DESC);
