-- Set when Google sign-in removes the password of an account nobody had verified. Such an account can still ask for a
-- reset link to choose a password again; an account that never had a password (created by Google sign-in) cannot.
ALTER TABLE users ADD COLUMN password_cleared_at TIMESTAMP;
