-- Compares user_db with learning_db for the tables copied by copy-study-data.sql. Read-only. Run in learning_db with
-- :src set to a libpq connection string for user_db. Prints one row per check and fails when learning_db is missing
-- data that user_db holds. Before traffic moves the two sides match exactly; afterwards learning_db may hold more.
-- Only counts and sums that can only grow are compared. The state of a goal can differ on purpose once learners use
-- Learning, so goals are compared by how many there are, not by how many are active.
\set ON_ERROR_STOP on
CREATE EXTENSION IF NOT EXISTS dblink;

CREATE TEMP TABLE reconciliation (check_name text, user_db bigint, learning_db bigint, ok boolean);

CREATE FUNCTION pg_temp.remote(conn text, sql text) RETURNS bigint LANGUAGE sql AS
$$ SELECT v FROM dblink(conn, sql) AS t(v bigint) $$;

INSERT INTO reconciliation
SELECT check_name, user_db, learning_db, learning_db >= user_db
FROM (VALUES
    ('learning_goals rows', pg_temp.remote(:'src', 'SELECT count(*) FROM learning_goals'), (SELECT count(*) FROM learning_goals)),
    ('learning_activities rows', pg_temp.remote(:'src', 'SELECT count(*) FROM learning_activities'), (SELECT count(*) FROM learning_activities)),
    ('study_sessions rows', pg_temp.remote(:'src', 'SELECT count(*) FROM study_sessions'), (SELECT count(*) FROM study_sessions)),
    ('study_sessions total_accepted_ms', pg_temp.remote(:'src', 'SELECT coalesce(sum(total_accepted_ms), 0) FROM study_sessions'),
        (SELECT coalesce(sum(total_accepted_ms), 0) FROM study_sessions)),
    ('study_heartbeat_receipts rows', pg_temp.remote(:'src', 'SELECT count(*) FROM study_heartbeat_receipts'),
        (SELECT count(*) FROM study_heartbeat_receipts)),
    ('study_activity_intervals active_ms', pg_temp.remote(:'src', 'SELECT coalesce(sum(active_ms), 0) FROM study_activity_intervals'),
        (SELECT coalesce(sum(active_ms), 0) FROM study_activity_intervals)),
    ('study_daily_totals active_ms', pg_temp.remote(:'src', 'SELECT coalesce(sum(active_ms), 0) FROM study_daily_totals'),
        (SELECT coalesce(sum(active_ms), 0) FROM study_daily_totals)),
    ('study_goal_history rows', pg_temp.remote(:'src', 'SELECT count(*) FROM study_goal_history'), (SELECT count(*) FROM study_goal_history))
) AS checks(check_name, user_db, learning_db);

-- The tracking start is the original one: learning_db may not start later than user_db did.
INSERT INTO reconciliation
SELECT 'study_tracking_policy not later than user_db', 0, 0,
       (SELECT started_at FROM study_tracking_policy) <= (SELECT started_at FROM dblink(:'src',
           'SELECT started_at FROM study_tracking_policy') AS t(started_at timestamptz));

SELECT check_name, user_db, learning_db, CASE WHEN ok THEN 'ok' ELSE 'MISMATCH' END AS result FROM reconciliation ORDER BY check_name;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM reconciliation WHERE NOT ok) THEN
        RAISE EXCEPTION 'learning_db is missing study data that user_db holds';
    END IF;
END $$;
