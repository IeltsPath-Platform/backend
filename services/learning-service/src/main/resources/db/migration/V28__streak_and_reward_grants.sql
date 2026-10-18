-- Streak of a learner, decided day by day up to evaluated_through. frozen_run_end and freeze_run describe the latest run
-- of frozen days, which sets the price of the next freeze.
CREATE TABLE learner_streaks (
    user_id UUID PRIMARY KEY,
    current_days INTEGER NOT NULL DEFAULT 0 CHECK (current_days >= 0),
    longest_days INTEGER NOT NULL DEFAULT 0 CHECK (longest_days >= 0),
    last_active_date DATE,
    streak_start DATE,
    evaluated_through DATE,
    freeze_available BOOLEAN NOT NULL DEFAULT FALSE,
    freeze_eligible_from DATE,
    frozen_run_end DATE,
    freeze_run INTEGER NOT NULL DEFAULT 0 CHECK (freeze_run >= 0),
    lost_streak_days INTEGER NOT NULL DEFAULT 0 CHECK (lost_streak_days >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Points owed to a learner by a reward, queued in the submission transaction and sent to Access by a relay. A reward is
-- queued once per reason and reference, so a repeated milestone adds nothing.
CREATE TABLE reward_grants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('STREAK_MILESTONE', 'KP_MASTERED', 'DAILY_QUEST', 'BADGE')),
    reference_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'GRANTED', 'CAPPED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    last_error VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, reason, reference_id)
);
CREATE INDEX idx_reward_grants_pending ON reward_grants (created_at) WHERE status = 'PENDING';

-- Streak milestones are XP sources too.
ALTER TABLE xp_ledger DROP CONSTRAINT xp_ledger_source_check;
ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_source_check
    CHECK (source IN ('QUESTION', 'KP_CONSOLIDATE', 'KP_MAINTAIN', 'STREAK'));
