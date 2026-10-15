CREATE TABLE study_tracking_policy (id integer PRIMARY KEY CHECK (id = 1), started_at timestamptz NOT NULL);
INSERT INTO study_tracking_policy VALUES (1, CURRENT_TIMESTAMP);

CREATE TABLE study_sessions (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 client_session_id uuid NOT NULL, source_type varchar(32) NOT NULL, source_id uuid NOT NULL,
 course_id uuid, timezone varchar(80) NOT NULL, epoch bigint NOT NULL CHECK (epoch > 0),
 started_at timestamptz NOT NULL, lease_expires_at timestamptz NOT NULL,
 sequence bigint NOT NULL DEFAULT 0, watermark_ms bigint NOT NULL DEFAULT 0,
 total_accepted_ms bigint NOT NULL DEFAULT 0 CHECK (total_accepted_ms >= 0),
 status varchar(10) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
 UNIQUE(user_id, client_session_id)
);
CREATE UNIQUE INDEX idx_study_sessions_open_user ON study_sessions(user_id) WHERE status = 'OPEN';

CREATE TABLE study_heartbeat_receipts (
 session_id uuid NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
 epoch bigint NOT NULL, sequence bigint NOT NULL, payload_hash varchar(64) NOT NULL,
 accepted_ms bigint NOT NULL, total_accepted_ms bigint NOT NULL,
 lease_expires_at timestamptz NOT NULL, status varchar(10) NOT NULL,
 PRIMARY KEY(session_id, epoch, sequence)
);
CREATE TABLE study_activity_intervals (
 session_id uuid NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
 epoch bigint NOT NULL, sequence bigint NOT NULL, segment integer NOT NULL,
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 started_at timestamptz NOT NULL, ended_at timestamptz NOT NULL,
 local_date date NOT NULL, timezone varchar(80) NOT NULL,
 active_ms bigint NOT NULL CHECK(active_ms > 0),
 CHECK(ended_at > started_at), PRIMARY KEY(session_id, epoch, sequence, segment)
);
CREATE INDEX idx_study_intervals_user_time ON study_activity_intervals(user_id, started_at);
CREATE TABLE study_daily_totals (
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE, local_date date NOT NULL,
 timezone varchar(80) NOT NULL, scope_key varchar(40) NOT NULL,
 active_ms bigint NOT NULL CHECK(active_ms >= 0), PRIMARY KEY(user_id, local_date, scope_key, timezone)
);
CREATE TABLE study_goal_history (
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE, effective_date date NOT NULL,
 timezone varchar(80) NOT NULL, effective_at timestamptz NOT NULL,
 goal_id uuid, minutes integer CHECK(minutes > 0), superseded_at timestamptz,
 PRIMARY KEY(user_id, effective_date)
);
-- Seed only the current target at activation; no inferred historical targets or study time.
INSERT INTO study_goal_history(user_id,effective_date,timezone,effective_at,goal_id,minutes)
SELECT g.user_id, (CURRENT_TIMESTAMP AT TIME ZONE COALESCE(t.name,'Asia/Ho_Chi_Minh'))::date,
 COALESCE(t.name,'Asia/Ho_Chi_Minh'),CURRENT_TIMESTAMP,g.id,g.available_minutes_per_day
FROM learning_goals g LEFT JOIN learner_profiles p ON p.user_id=g.user_id
LEFT JOIN pg_timezone_names t ON t.name=p.timezone WHERE g.status='ACTIVE';
