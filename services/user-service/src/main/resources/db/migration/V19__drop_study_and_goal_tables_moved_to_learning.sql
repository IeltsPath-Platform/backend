-- Goals, activities, study time and streaks moved to learning-service (learning_db); nothing in this service reads or
-- writes these tables any more. The two child tables of study_sessions go first.
DROP TABLE IF EXISTS study_heartbeat_receipts;
DROP TABLE IF EXISTS study_activity_intervals;
DROP TABLE IF EXISTS study_sessions;
DROP TABLE IF EXISTS study_daily_totals;
DROP TABLE IF EXISTS study_tracking_policy;
DROP TABLE IF EXISTS study_goal_history;
DROP TABLE IF EXISTS learning_goals;
DROP TABLE IF EXISTS learning_activities;
DROP TABLE IF EXISTS streaks;
