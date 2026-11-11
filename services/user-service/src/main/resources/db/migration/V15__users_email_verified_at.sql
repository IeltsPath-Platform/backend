-- Accounts that already exist were created before verification existed: they count as verified.
-- New accounts start unverified until the email link is used (or a completed password reset proves the address).
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;
UPDATE users SET email_verified_at = created_at;
