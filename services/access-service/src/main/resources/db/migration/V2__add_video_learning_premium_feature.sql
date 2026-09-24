INSERT INTO plan_features (plan_id, feature_key, is_enabled)
SELECT id, 'VIDEO_LEARNING_PREMIUM', true
FROM plans
WHERE code = 'PREMIUM'
ON CONFLICT (plan_id, feature_key)
    DO UPDATE SET is_enabled = true,
                  updated_at = CURRENT_TIMESTAMP;
