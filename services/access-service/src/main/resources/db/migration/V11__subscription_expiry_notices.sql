-- Contact details travel with the verification state so expiry notices can be addressed without calling user-service.
ALTER TABLE user_email_verification_state ADD COLUMN email VARCHAR(255);
ALTER TABLE user_email_verification_state ADD COLUMN display_name VARCHAR(150);

-- One row per subscription and stage: each expiry notice is claimed here before it is queued, so it goes out once.
CREATE TABLE subscription_expiry_notices (
    subscription_id UUID NOT NULL,
    stage VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (subscription_id, stage)
);
