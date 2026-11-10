CREATE TABLE practice_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('WRITING', 'SPEAKING', 'VOCABULARY')),
    request_id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    UNIQUE (user_id, request_id)
);
