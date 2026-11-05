-- Copies goals, learning activity and study time from user_db into learning_db. Run in learning_db with :src set to a
-- libpq connection string for user_db (see ../copy-study-data-to-learning.sh). Nothing is written to user_db.
--
-- Safe to run again: rows are matched by key, a row already in learning_db is replaced only when the copy from
-- user_db is further along, and rows are never deleted. So a final run after traffic moved to Learning cannot
-- overwrite what Learning has recorded since.
\set ON_ERROR_STOP on
CREATE EXTENSION IF NOT EXISTS dblink;

BEGIN;

-- Goals. At most one goal per learner may be ACTIVE, so goals that are not active go first and may demote a goal that
-- was active in an earlier copy; the active goals follow.
CREATE TEMP TABLE src_goals ON COMMIT DROP AS
SELECT * FROM dblink(:'src', $q$
    SELECT id, user_id, target_band, exam_date, available_minutes_per_day, status, started_at, ended_at, created_at, updated_at
    FROM learning_goals
$q$) AS s(id uuid, user_id uuid, target_band numeric(3, 1), exam_date date, available_minutes_per_day integer,
          status varchar(30), started_at timestamp, ended_at timestamp, created_at timestamp, updated_at timestamp);

INSERT INTO learning_goals AS t (id, user_id, target_band, exam_date, available_minutes_per_day, status, started_at,
                                 ended_at, created_at, updated_at)
SELECT * FROM src_goals WHERE status <> 'ACTIVE'
ON CONFLICT (id) DO UPDATE SET target_band = EXCLUDED.target_band, exam_date = EXCLUDED.exam_date,
    available_minutes_per_day = EXCLUDED.available_minutes_per_day, status = EXCLUDED.status,
    started_at = EXCLUDED.started_at, ended_at = EXCLUDED.ended_at, updated_at = EXCLUDED.updated_at
WHERE t.updated_at < EXCLUDED.updated_at;

INSERT INTO learning_goals AS t (id, user_id, target_band, exam_date, available_minutes_per_day, status, started_at,
                                 ended_at, created_at, updated_at)
SELECT * FROM src_goals WHERE status = 'ACTIVE'
ON CONFLICT (id) DO UPDATE SET target_band = EXCLUDED.target_band, exam_date = EXCLUDED.exam_date,
    available_minutes_per_day = EXCLUDED.available_minutes_per_day, status = EXCLUDED.status,
    started_at = EXCLUDED.started_at, ended_at = EXCLUDED.ended_at, updated_at = EXCLUDED.updated_at
WHERE t.updated_at < EXCLUDED.updated_at;

-- Learning activity never changes once written.
INSERT INTO learning_activities (id, user_id, activity_type, source_type, source_id, occurred_at, duration_seconds, verified_at)
SELECT * FROM dblink(:'src', $q$
    SELECT id, user_id, activity_type, source_type, source_id, occurred_at, duration_seconds, verified_at
    FROM learning_activities
$q$) AS s(id uuid, user_id uuid, activity_type varchar(50), source_type varchar(50), source_id uuid,
          occurred_at timestamptz, duration_seconds integer, verified_at timestamptz)
ON CONFLICT (id) DO NOTHING;

-- The moment study tracking began is the original one, not the moment learning_db was migrated.
INSERT INTO study_tracking_policy AS t (id, started_at)
SELECT * FROM dblink(:'src', 'SELECT id, started_at FROM study_tracking_policy') AS s(id integer, started_at timestamptz)
ON CONFLICT (id) DO UPDATE SET started_at = LEAST(t.started_at, EXCLUDED.started_at);

-- Sessions. A learner has one OPEN session, so closed ones go first, as with goals. A copy is further along when its
-- epoch, sequence, accepted watermark or state is ahead.
CREATE TEMP TABLE src_sessions ON COMMIT DROP AS
SELECT * FROM dblink(:'src', $q$
    SELECT id, user_id, client_session_id, source_type, source_id, course_id, timezone, epoch, started_at,
           lease_expires_at, sequence, watermark_ms, total_accepted_ms, status
    FROM study_sessions
$q$) AS s(id uuid, user_id uuid, client_session_id uuid, source_type varchar(32), source_id uuid, course_id uuid,
          timezone varchar(80), epoch bigint, started_at timestamptz, lease_expires_at timestamptz, sequence bigint,
          watermark_ms bigint, total_accepted_ms bigint, status varchar(10));

INSERT INTO study_sessions AS t SELECT * FROM src_sessions WHERE status = 'CLOSED'
ON CONFLICT (id) DO UPDATE SET lease_expires_at = EXCLUDED.lease_expires_at, epoch = EXCLUDED.epoch,
    sequence = EXCLUDED.sequence, watermark_ms = EXCLUDED.watermark_ms,
    total_accepted_ms = EXCLUDED.total_accepted_ms, status = EXCLUDED.status
WHERE (EXCLUDED.epoch, EXCLUDED.sequence, EXCLUDED.watermark_ms, (EXCLUDED.status = 'CLOSED')::int)
    > (t.epoch, t.sequence, t.watermark_ms, (t.status = 'CLOSED')::int);

INSERT INTO study_sessions AS t SELECT * FROM src_sessions WHERE status = 'OPEN'
ON CONFLICT (id) DO UPDATE SET lease_expires_at = EXCLUDED.lease_expires_at, epoch = EXCLUDED.epoch,
    sequence = EXCLUDED.sequence, watermark_ms = EXCLUDED.watermark_ms,
    total_accepted_ms = EXCLUDED.total_accepted_ms, status = EXCLUDED.status
WHERE (EXCLUDED.epoch, EXCLUDED.sequence, EXCLUDED.watermark_ms, (EXCLUDED.status = 'CLOSED')::int)
    > (t.epoch, t.sequence, t.watermark_ms, (t.status = 'CLOSED')::int);

-- Receipts and intervals are written once.
INSERT INTO study_heartbeat_receipts
SELECT * FROM dblink(:'src', $q$
    SELECT session_id, epoch, sequence, payload_hash, accepted_ms, total_accepted_ms, lease_expires_at, status
    FROM study_heartbeat_receipts
$q$) AS s(session_id uuid, epoch bigint, sequence bigint, payload_hash varchar(64), accepted_ms bigint,
          total_accepted_ms bigint, lease_expires_at timestamptz, status varchar(10))
ON CONFLICT DO NOTHING;

INSERT INTO study_activity_intervals
SELECT * FROM dblink(:'src', $q$
    SELECT session_id, epoch, sequence, segment, user_id, started_at, ended_at, local_date, timezone, active_ms
    FROM study_activity_intervals
$q$) AS s(session_id uuid, epoch bigint, sequence bigint, segment integer, user_id uuid, started_at timestamptz,
          ended_at timestamptz, local_date date, timezone varchar(80), active_ms bigint)
ON CONFLICT DO NOTHING;

-- Daily totals only grow.
INSERT INTO study_daily_totals AS t
SELECT * FROM dblink(:'src', 'SELECT user_id, local_date, timezone, scope_key, active_ms FROM study_daily_totals')
    AS s(user_id uuid, local_date date, timezone varchar(80), scope_key varchar(40), active_ms bigint)
ON CONFLICT (user_id, local_date, scope_key, timezone) DO UPDATE SET active_ms = EXCLUDED.active_ms
WHERE EXCLUDED.active_ms > t.active_ms;

-- A goal schedule is replaced when the copy is newer or has since been superseded.
INSERT INTO study_goal_history AS t
SELECT * FROM dblink(:'src', $q$
    SELECT user_id, effective_date, timezone, effective_at, goal_id, minutes, superseded_at FROM study_goal_history
$q$) AS s(user_id uuid, effective_date date, timezone varchar(80), effective_at timestamptz, goal_id uuid,
          minutes integer, superseded_at timestamptz)
ON CONFLICT (user_id, effective_date) DO UPDATE SET timezone = EXCLUDED.timezone, effective_at = EXCLUDED.effective_at,
    goal_id = EXCLUDED.goal_id, minutes = EXCLUDED.minutes, superseded_at = EXCLUDED.superseded_at
WHERE (EXCLUDED.effective_at, COALESCE(EXCLUDED.superseded_at, '-infinity'))
    > (t.effective_at, COALESCE(t.superseded_at, '-infinity'));

COMMIT;
