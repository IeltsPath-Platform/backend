CREATE INDEX idx_login_history_retention_created_id
    ON login_history (created_at, id);
