ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE oauth_identities
    ADD CONSTRAINT uq_oauth_user_provider UNIQUE (user_id, provider);
