-- Demo data for the learning side of the acceptance demos. Run it against learning_db with psql, for example:
--   psql -h localhost -U <user> -d learning_db -v demo_user=<uuid of a demo learner> -f demo-data-learning.sql
-- It only writes rows for demo learners (random ids and the learner you pass) and never truncates anything. Run it once
-- per clean database: a second run adds a second league of 31 learners in the same week.

-- Gamification demo 3 and 4: a learner on a 6-day streak whose last active day was yesterday (UTC). One answer today makes
-- the 7th day: +100 XP, 10 points queued, badge STREAK_7. Run the lines after "demo 4" instead to test a freeze: a learner
-- with a 5-day streak who owns a freeze and skips one day keeps the streak.
INSERT INTO learner_streaks (user_id, current_days, longest_days, last_active_date, streak_start, evaluated_through,
                             freeze_available, freeze_run, lost_streak_days)
VALUES (:'demo_user', 6, 6, CURRENT_DATE - 1, CURRENT_DATE - 6, CURRENT_DATE - 1, FALSE, 0, 0)
ON CONFLICT (user_id) DO UPDATE SET current_days = 6, longest_days = 6, last_active_date = CURRENT_DATE - 1,
    streak_start = CURRENT_DATE - 6, evaluated_through = CURRENT_DATE - 1, freeze_available = FALSE, freeze_run = 0,
    lost_streak_days = 0, updated_at = CURRENT_TIMESTAMP;

-- demo 4 (use a second learner, set -v demo_user_freeze=<uuid>): streak of 5 ending two days ago, freeze owned and
-- eligible from yesterday, so yesterday is covered and the streak is still alive when the learner answers today.
-- INSERT INTO learner_streaks (user_id, current_days, longest_days, last_active_date, streak_start, evaluated_through,
--                              freeze_available, freeze_eligible_from, freeze_run)
-- VALUES (:'demo_user_freeze', 5, 5, CURRENT_DATE - 2, CURRENT_DATE - 6, CURRENT_DATE - 2, TRUE, CURRENT_DATE - 1, 0);

-- Gamification demo 6: 31 learners with XP in one past week, tier 1 (Bronze). The first 30 fill one group, the 31st opens a
-- second. Weeks start on Sunday (UTC); the week used is the one two Sundays ago, so it is already over. Closing it (the
-- league close job; set learning.league.close-cron / LEARNING_LEAGUE_CLOSE_CRON to "0 * * * * *" to run it every minute)
-- promotes ranks 1-7 of each group and, for the group of 30, demotes ranks 23-30 (Bronze stays Bronze).
DO $$
DECLARE
    week DATE := date_trunc('week', CURRENT_DATE + 1)::date - 1 - 14;   -- Sunday two weeks ago
    big UUID := gen_random_uuid();
    small UUID := gen_random_uuid();
    learner UUID;
    n INTEGER;
BEGIN
    INSERT INTO league_weeks (week_start) VALUES (week) ON CONFLICT DO NOTHING;
    INSERT INTO league_groups (id, week_start, tier) VALUES (big, week, 1), (small, week, 1);
    FOR n IN 1..31 LOOP
        learner := gen_random_uuid();
        INSERT INTO league_members (week_start, user_id, group_id, tier, weekly_xp, xp_updated_at)
        VALUES (week, learner, CASE WHEN n <= 30 THEN big ELSE small END, 1, n * 10,
                TIMESTAMPTZ '2026-01-01 00:00:00+00' + (n || ' seconds')::interval);
    END LOOP;
END $$;

-- Check after the close job ran:
--   SELECT outcome, count(*) FROM league_members GROUP BY outcome;          -- PROMOTED/STAYED/DEMOTED
--   SELECT tier, count(*) FROM learner_league GROUP BY tier;                -- promoted learners moved to tier 2
