-- Latest known email-verification state per account, fed by user-service. An account with no row is treated as allowed:
-- accounts that existed before verification were marked verified there, and only unverified accounts get a row with
-- verified = false.
CREATE TABLE user_email_verification_state (
    user_id UUID PRIMARY KEY,
    verified BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
