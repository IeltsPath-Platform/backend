ALTER TABLE refresh_tokens
    ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN session_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN revoked_reason VARCHAR(20),
    ADD COLUMN replaced_by_id UUID,
    ADD COLUMN last_used_at TIMESTAMP,
    ADD COLUMN ip_address VARCHAR(45),
    ADD COLUMN user_agent VARCHAR(255),
    ADD CONSTRAINT chk_refresh_tokens_revoked_reason CHECK (revoked_reason IS NULL OR revoked_reason IN
        ('ROTATED','LOGOUT','PASSWORD_CHANGE','PASSWORD_RESET','GOOGLE_LINK','SESSION_REVOKED','REUSE_DETECTED','ADMIN'));

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;
