-- Preserve the existing API's most-recent-active selection when repairing historical duplicates.
WITH ranked_active_goals AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC, id DESC) AS position
    FROM learning_goals
    WHERE status = 'ACTIVE'
)
UPDATE learning_goals AS goal
SET status = 'PAUSED',
    updated_at = CURRENT_TIMESTAMP
FROM ranked_active_goals AS ranked
WHERE goal.id = ranked.id
  AND ranked.position > 1;

CREATE UNIQUE INDEX uq_learning_goals_one_active_per_user
    ON learning_goals (user_id)
    WHERE status = 'ACTIVE';
