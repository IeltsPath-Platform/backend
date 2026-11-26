DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY lower(email)
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize users.email: case-insensitive duplicates exist; resolve accounts before applying this migration';
    END IF;
END
$$;

UPDATE users
SET email = lower(email)
WHERE email <> lower(email);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
