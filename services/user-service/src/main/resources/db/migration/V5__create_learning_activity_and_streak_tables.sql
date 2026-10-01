CREATE TABLE learning_activities (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    activity_type varchar(50) NOT NULL,
    source_type varchar(50) NOT NULL,
    source_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    duration_seconds integer,
    verified_at timestamptz,
    CONSTRAINT chk_learning_activities_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX idx_learning_activities_user_occurred
    ON learning_activities (user_id, occurred_at DESC);

CREATE TABLE streaks (
    user_id uuid PRIMARY KEY,
    current_days integer NOT NULL DEFAULT 0,
    longest_days integer NOT NULL DEFAULT 0,
    last_qualified_date date,
    timezone varchar(100) NOT NULL
);
