-- V2__create_identity_tables.sql

-- 1. learner_profiles: Hồ sơ học tập chuyên sâu của learner (1-1 với users)
CREATE TABLE learner_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    avatar_reference VARCHAR(500),
    bio TEXT,
    self_reported_band NUMERIC(3, 1),
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    profile_visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learner_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_learner_profiles_visibility CHECK (profile_visibility IN ('PUBLIC', 'PRIVATE', 'COMMUNITY'))
);

-- 2. oauth_identities: Liên kết danh tính OAuth (Google, Facebook, Apple, v.v.)
CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_authenticated_at TIMESTAMP,
    CONSTRAINT fk_oauth_identities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_oauth_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_oauth_identities_user_id ON oauth_identities (user_id);

-- 3. account_action_tokens: Token một lần cho quên mật khẩu, kích hoạt, xác thực email
CREATE TABLE account_action_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_action_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_account_action_tokens_purpose CHECK (purpose IN ('PASSWORD_RESET', 'EMAIL_VERIFICATION', 'ACCOUNT_ACTIVATION'))
);

CREATE INDEX idx_account_action_tokens_user_id ON account_action_tokens (user_id);
CREATE INDEX idx_account_action_tokens_active
    ON account_action_tokens (token_hash)
    WHERE used_at IS NULL;

-- 4. learning_goals: Lưu mục tiêu học của learner
CREATE TABLE learning_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    target_band NUMERIC(3, 1) NOT NULL,
    exam_date DATE,
    available_minutes_per_day INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learning_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_learning_goals_status CHECK (status IN ('ACTIVE', 'ACHIEVED', 'ABANDONED', 'PAUSED'))
);

CREATE INDEX idx_learning_goals_user_id ON learning_goals (user_id);

