INSERT INTO plan_features (plan_id, feature_key, is_enabled)
VALUES ('22222222-2222-2222-2222-222222222222', 'HUMAN_GRADING_UNLIMITED', true);

ALTER TABLE human_grading_credit_consumptions
    ADD COLUMN credit_spent BOOLEAN NOT NULL DEFAULT true;
