ALTER TABLE point_ledger_entries
    DROP CONSTRAINT IF EXISTS chk_point_ledger_entries_transaction_type;

ALTER TABLE point_ledger_entries
    ADD CONSTRAINT chk_point_ledger_entries_transaction_type
        CHECK (transaction_type IN (
            'KEY_CREDIT',
            'AI_GRADING_DEBIT',
            'AI_GRADING_REFUND',
            'ADMIN_ADJUSTMENT',
            'LEARNING_REWARD',
            'HEART_REFILL_DEBIT',
            'STREAK_FREEZE_DEBIT',
            'COURSE_UNLOCK_DEBIT'
        ));
