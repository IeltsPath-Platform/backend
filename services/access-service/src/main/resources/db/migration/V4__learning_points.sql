ALTER TABLE point_ledger_entries
    DROP CONSTRAINT IF EXISTS point_ledger_entries_transaction_type_check;

ALTER TABLE point_ledger_entries
    ALTER COLUMN reference_id TYPE VARCHAR(100) USING reference_id::text;

ALTER TABLE point_ledger_entries
    ADD CONSTRAINT chk_point_ledger_entries_transaction_type
        CHECK (transaction_type IN (
            'KEY_CREDIT',
            'AI_GRADING_DEBIT',
            'AI_GRADING_REFUND',
            'ADMIN_ADJUSTMENT',
            'LEARNING_REWARD',
            'HEART_REFILL_DEBIT',
            'STREAK_FREEZE_DEBIT'
        ));

CREATE TABLE point_credit_requests (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL CHECK (amount BETWEEN 1 AND 100),
    reason VARCHAR(32) NOT NULL CHECK (reason IN ('STREAK_MILESTONE', 'KP_MASTERED', 'DAILY_QUEST', 'BADGE')),
    reference_id VARCHAR(100) NOT NULL,
    granted INTEGER NOT NULL CHECK (granted BETWEEN 0 AND amount),
    capped BOOLEAN NOT NULL,
    balance BIGINT NOT NULL CHECK (balance >= 0),
    transaction_id UUID REFERENCES point_ledger_entries(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_point_credit_requests_user_created
    ON point_credit_requests (user_id, created_at DESC);
