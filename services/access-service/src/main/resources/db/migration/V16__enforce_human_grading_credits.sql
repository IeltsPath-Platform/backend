-- Requests accepted while Premium grading was unlimited become package-credit usage.
-- Requests already refunded never spent a credit and remain uncharged.
WITH newly_charged AS (
    UPDATE human_grading_credit_consumptions
    SET credit_spent = true
    WHERE credit_spent = false AND refunded_at IS NULL
    RETURNING subscription_id
), usage AS (
    SELECT subscription_id, COUNT(*)::INT AS credits_used
    FROM newly_charged
    GROUP BY subscription_id
)
UPDATE subscriptions s
SET human_grading_credits_used = s.human_grading_credits_used + usage.credits_used,
    updated_at = CURRENT_TIMESTAMP,
    row_version = s.row_version + 1
FROM usage
WHERE s.id = usage.subscription_id;

DELETE FROM plan_features
WHERE plan_id = '22222222-2222-2222-2222-222222222222'
  AND feature_key = 'HUMAN_GRADING_UNLIMITED';
