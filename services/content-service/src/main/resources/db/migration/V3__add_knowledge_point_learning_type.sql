ALTER TABLE knowledge_points
    ADD COLUMN learning_type VARCHAR(20);

ALTER TABLE knowledge_points
    ADD CONSTRAINT chk_knowledge_points_learning_type
        CHECK (learning_type IS NULL OR learning_type IN ('MEMORY', 'CONCEPT', 'PROCEDURE', 'DESIGN'));

-- Existing active rows cannot be mapped safely from kind (GRAMMAR/VOCABULARY/etc.).
-- Keep their records, but make them unavailable until an editor classifies and republishes them.
UPDATE knowledge_points
SET status = 'INACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'ACTIVE'
  AND learning_type IS NULL;

ALTER TABLE knowledge_points
    ADD CONSTRAINT chk_active_knowledge_points_require_learning_type
        CHECK (status <> 'ACTIVE' OR learning_type IS NOT NULL);
