UPDATE subscriptions
SET status = 'EXPIRED', updated_at = now()
WHERE status = 'ACTIVE'
  AND ends_at IS NOT NULL
  AND ends_at <= now();

WITH ranked_active AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id
               ORDER BY ends_at DESC NULLS FIRST, created_at DESC, id
           ) AS position
    FROM subscriptions
    WHERE status = 'ACTIVE'
)
UPDATE subscriptions AS subscription
SET status = 'CANCELLED',
    cancelled_at = now(),
    updated_at = now()
FROM ranked_active
WHERE subscription.id = ranked_active.id
  AND ranked_active.position > 1;

CREATE UNIQUE INDEX uq_subscriptions_one_active
    ON subscriptions (user_id)
    WHERE status = 'ACTIVE';
