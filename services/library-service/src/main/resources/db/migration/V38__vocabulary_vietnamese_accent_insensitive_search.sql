-- Vietnamese meanings are searched without regard to accents: "o nhiem" finds "ô nhiễm", and "ô nhiễm" still does.
--
-- unaccent() is only STABLE (its dictionary could change), so an index cannot call it directly. vocabulary_fold wraps it
-- with the dictionary named explicitly and is declared IMMUTABLE, the usual way to index unaccented text. It also
-- lower-cases, so one function gives the form both the index and the query compare. unaccent maps đ/Đ to d.
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION vocabulary_fold(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, $1)) $$;

-- Replaces the accent-sensitive index of V37, which no query uses any more.
DROP INDEX IF EXISTS idx_vocabulary_senses_active_meaning_trgm;

CREATE INDEX idx_vocabulary_senses_active_meaning_fold_trgm
    ON vocabulary_senses USING gin (vocabulary_fold(vietnamese_meaning) gin_trgm_ops)
    WHERE status = 'ACTIVE';
