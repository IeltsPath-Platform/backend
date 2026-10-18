-- Learner timezone; the daily XP cap and the streak days are counted in it.
CREATE TABLE learner_settings (
    user_id UUID PRIMARY KEY,
    timezone VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- XP ledger. A row per awarded answer or knowledge-point milestone. Counted rows make the total; rows past the daily
-- cap are kept as shadow XP (counted = false) so the learner's history shows what was not counted.
CREATE TABLE xp_ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    source VARCHAR(30) NOT NULL CHECK (source IN ('QUESTION', 'KP_CONSOLIDATE', 'KP_MAINTAIN')),
    source_ref VARCHAR(120) NOT NULL,
    local_date DATE NOT NULL,
    counted BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, source_ref)
);
CREATE INDEX idx_xp_ledger_day ON xp_ledger (user_id, local_date) WHERE counted;

CREATE TABLE learner_xp (
    user_id UUID PRIMARY KEY,
    total_xp INTEGER NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
