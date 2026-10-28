-- Failed current-password attempts of the "change password" endpoint, kept in the database so the limit holds
-- across service instances and restarts. One row per user, removed with the user and after a successful change.
CREATE TABLE password_change_attempts (
    user_id UUID PRIMARY KEY,
    failures INT NOT NULL,
    window_started_at TIMESTAMP NOT NULL,
    locked_until TIMESTAMP,
    CONSTRAINT fk_password_change_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_password_change_attempts_failures CHECK (failures >= 0)
);
