CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_vocabulary_items_active_lemma_prefix
    ON vocabulary_items (normalized_lemma varchar_pattern_ops)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_vocabulary_items_active_lemma_trgm
    ON vocabulary_items USING gin (normalized_lemma gin_trgm_ops)
    WHERE status = 'ACTIVE';
