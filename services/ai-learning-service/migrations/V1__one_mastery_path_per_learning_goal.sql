-- A goal-bound mastery path is unique for its learner. Ungrouped/ad-hoc paths may repeat.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM mastery_paths
        WHERE learning_goal_id IS NOT NULL
        GROUP BY user_id, learning_goal_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate goal-bound mastery paths exist; reconcile them before applying this migration';
    END IF;
END
$$;

CREATE UNIQUE INDEX uq_mastery_paths_user_learning_goal
    ON mastery_paths (user_id, learning_goal_id)
    WHERE learning_goal_id IS NOT NULL;
