-- A learner's standing at the end of a week or a month: mastery per skill, tier counts, what they answered and how many
-- minutes they were active. The weekly row is kept 12 weeks and the monthly row 36 months. Active minutes cap each answer
-- at ten minutes, so a question left open does not inflate them.
CREATE TABLE learner_weekly_snapshots (
    user_id UUID NOT NULL,
    week_start DATE NOT NULL,
    skills JSONB NOT NULL,
    tier_counts JSONB NOT NULL,
    answered INTEGER NOT NULL CHECK (answered >= 0),
    correct INTEGER NOT NULL CHECK (correct >= 0),
    active_minutes INTEGER NOT NULL CHECK (active_minutes >= 0),
    focus_kp_ids UUID[] NOT NULL DEFAULT '{}',
    estimated_band NUMERIC(2,1) CHECK (estimated_band IS NULL OR (estimated_band BETWEEN 0 AND 9
        AND estimated_band * 2 = trunc(estimated_band * 2))),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, week_start),
    CHECK (correct <= answered)
);

CREATE TABLE learner_monthly_snapshots (
    user_id UUID NOT NULL,
    month_start DATE NOT NULL,
    skills JSONB NOT NULL,
    tier_counts JSONB NOT NULL,
    answered INTEGER NOT NULL CHECK (answered >= 0),
    correct INTEGER NOT NULL CHECK (correct >= 0),
    active_minutes INTEGER NOT NULL CHECK (active_minutes >= 0),
    focus_kp_ids UUID[] NOT NULL DEFAULT '{}',
    estimated_band NUMERIC(2,1) CHECK (estimated_band IS NULL OR (estimated_band BETWEEN 0 AND 9
        AND estimated_band * 2 = trunc(estimated_band * 2))),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, month_start),
    CHECK (correct <= answered)
);
