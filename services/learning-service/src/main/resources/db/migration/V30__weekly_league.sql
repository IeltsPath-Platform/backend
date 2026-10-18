-- Weekly league. A week runs from Sunday 00:00 UTC. Learners with XP that week sit in a group of up to 30 of the same tier
-- and are ranked by weekly XP; when two have the same XP, whoever reached it first ranks higher. Tiers are stored as
-- 1 (lowest) to 7 (highest) so closing a week can move whole groups with one statement.
CREATE TABLE league_weeks (
    week_start DATE PRIMARY KEY,
    closed_at TIMESTAMPTZ
);

CREATE TABLE league_groups (
    id UUID PRIMARY KEY,
    week_start DATE NOT NULL REFERENCES league_weeks (week_start),
    tier SMALLINT NOT NULL CHECK (tier BETWEEN 1 AND 7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_league_groups_week_tier ON league_groups (week_start, tier, created_at);

CREATE TABLE league_members (
    week_start DATE NOT NULL REFERENCES league_weeks (week_start),
    user_id UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES league_groups (id),
    tier SMALLINT NOT NULL CHECK (tier BETWEEN 1 AND 7),
    weekly_xp INTEGER NOT NULL CHECK (weekly_xp >= 0),
    xp_updated_at TIMESTAMPTZ NOT NULL,
    final_rank INTEGER CHECK (final_rank IS NULL OR final_rank >= 1),
    outcome VARCHAR(10) CHECK (outcome IS NULL OR outcome IN ('PROMOTED', 'STAYED', 'DEMOTED')),
    PRIMARY KEY (week_start, user_id)
);
CREATE INDEX idx_league_members_group ON league_members (group_id, weekly_xp DESC);
CREATE INDEX idx_league_members_user ON league_members (user_id, week_start DESC);

-- The tier a learner plays in next. A learner with no row plays in tier 1.
CREATE TABLE learner_league (
    user_id UUID PRIMARY KEY,
    tier SMALLINT NOT NULL CHECK (tier BETWEEN 1 AND 7),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
