-- Search indexes for a catalog of about 20,000 words.
--
-- Every word search keeps only words that have an active sense (EXISTS on vocabulary_senses). With the plain index on
-- vocabulary_item_id the planner hashed every sense row for broad terms ("ent", "tion"); an index holding only the
-- active senses lets it probe them by word instead.
CREATE INDEX idx_vocabulary_senses_active_item
    ON vocabulary_senses (vocabulary_item_id)
    WHERE status = 'ACTIVE';

-- The Vietnamese catalog search matches lower(vietnamese_meaning) LIKE '%...%', which no B-tree can serve.
-- pg_trgm is created in V36.
CREATE INDEX idx_vocabulary_senses_active_meaning_trgm
    ON vocabulary_senses USING gin (lower(vietnamese_meaning) gin_trgm_ops)
    WHERE status = 'ACTIVE';
