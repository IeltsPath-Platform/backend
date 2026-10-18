CREATE TABLE human_grading_credit_consumptions (
    submission_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
    remaining_credits INT NOT NULL CHECK (remaining_credits >= 0),
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at TIMESTAMPTZ
);

CREATE INDEX idx_human_grading_credit_consumptions_user ON human_grading_credit_consumptions(user_id, consumed_at DESC);
