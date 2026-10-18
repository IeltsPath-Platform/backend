-- The three quests of a learner's day, made on first use in the learner's own timezone. A quest is rewarded once, when it
-- is completed: the XP goes through the XP ledger and the points through the reward queue, both keyed by the quest id.
CREATE TABLE daily_quests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    local_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PRACTICE', 'WEAKNESS', 'PRACTICE_PLUS', 'STREAK')),
    target_kp_id UUID,
    required INTEGER NOT NULL CHECK (required >= 1),
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= required),
    completed_at TIMESTAMPTZ,
    xp_reward INTEGER NOT NULL CHECK (xp_reward >= 0),
    points_reward INTEGER NOT NULL DEFAULT 0 CHECK (points_reward >= 0),
    UNIQUE (user_id, local_date, type)
);

ALTER TABLE xp_ledger DROP CONSTRAINT xp_ledger_source_check;
ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_source_check
    CHECK (source IN ('QUESTION', 'KP_CONSOLIDATE', 'KP_MAINTAIN', 'STREAK', 'QUEST'));
