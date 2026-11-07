CREATE TABLE vocabulary_study_profiles (
    user_id UUID PRIMARY KEY,timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    total_xp INT NOT NULL DEFAULT 0 CHECK(total_xp>=0),level INT NOT NULL DEFAULT 1 CHECK(level>=1),
    hearts INT NOT NULL,max_hearts INT NOT NULL CHECK(max_hearts>=1),hearts_regen_at TIMESTAMPTZ NOT NULL,
    max_hearts_checked_at TIMESTAMPTZ,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(hearts>=0 AND hearts<=max_hearts)
);
CREATE TABLE vocabulary_study_xp_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),user_id UUID NOT NULL,
    source VARCHAR(30) NOT NULL CHECK(source IN ('UNIT_FIRST_COMPLETION','UNIT_PERFECT_BONUS')),
    source_ref VARCHAR(120) NOT NULL,amount INT NOT NULL CHECK(amount>0),local_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(user_id,source,source_ref)
);
CREATE TABLE vocabulary_study_streaks (
    user_id UUID PRIMARY KEY,current_days INT NOT NULL DEFAULT 0 CHECK(current_days>=0),
    longest_days INT NOT NULL DEFAULT 0 CHECK(longest_days>=0),last_active_date DATE,streak_start DATE,evaluated_through DATE,
    freeze_available BOOLEAN NOT NULL DEFAULT FALSE,freeze_eligible_from DATE,frozen_run_end DATE,
    freeze_run INT NOT NULL DEFAULT 0 CHECK(freeze_run>=0),lost_streak_days INT NOT NULL DEFAULT 0 CHECK(lost_streak_days>=0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE vocabulary_study_purchases (
    request_id UUID PRIMARY KEY,user_id UUID NOT NULL,kind VARCHAR(20) NOT NULL CHECK(kind IN ('HEART_REFILL','STREAK_FREEZE')),
    amount INT NOT NULL CHECK(amount>0),status VARCHAR(10) NOT NULL CHECK(status IN ('PENDING','DONE')),
    access_ledger_id UUID,balance_after BIGINT,result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vocab_study_purchase_user ON vocabulary_study_purchases(user_id,created_at DESC);
