CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1. PLANS & PLAN FEATURES
-- ============================================================================

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plan_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    limit_value INT,
    config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plan_feature UNIQUE (plan_id, feature_key)
);

CREATE INDEX idx_plan_features_plan_id ON plan_features(plan_id);

-- ============================================================================
-- 2. SUBSCRIPTIONS
-- ============================================================================

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES plans(id),
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    source_type VARCHAR(50) NOT NULL CHECK (source_type IN ('ACTIVATION_KEY', 'ADMIN')),
    source_reference_id UUID,
    human_grading_credits_total INT NOT NULL DEFAULT 0 CHECK (human_grading_credits_total >= 0),
    human_grading_credits_used INT NOT NULL DEFAULT 0 CHECK (human_grading_credits_used >= 0),
    cancelled_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);

-- ============================================================================
-- 3. KEY PRODUCTS & ACTIVATION KEYS
-- ============================================================================

CREATE TABLE key_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    key_type VARCHAR(50) NOT NULL CHECK (key_type IN ('POINTS', 'PREMIUM')),
    points_amount INT,
    plan_id UUID REFERENCES plans(id),
    premium_days INT,
    human_grading_credits INT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activation_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES key_products(id) ON DELETE RESTRICT,
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    code_hint VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REDEEMED', 'REVOKED', 'EXPIRED')),
    expires_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    redeemed_at TIMESTAMPTZ
);

CREATE INDEX idx_activation_keys_code_hash ON activation_keys(code_hash);
CREATE INDEX idx_activation_keys_product_id ON activation_keys(product_id);
CREATE INDEX idx_activation_keys_status ON activation_keys(status);

CREATE TABLE key_activations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_id UUID NOT NULL UNIQUE REFERENCES activation_keys(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL,
    product_type VARCHAR(50) NOT NULL CHECK (product_type IN ('POINTS', 'PREMIUM')),
    points_granted INT NOT NULL DEFAULT 0,
    premium_days_granted INT NOT NULL DEFAULT 0,
    human_grading_credits_granted INT NOT NULL DEFAULT 0,
    activated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_key_activations_user_id ON key_activations(user_id);
CREATE INDEX idx_key_activations_idempotency ON key_activations(idempotency_key);

-- ============================================================================
-- 4. POINT WALLETS & POINT LEDGER ENTRIES
-- ============================================================================

CREATE TABLE point_wallets (
    user_id UUID PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    total_credited BIGINT NOT NULL DEFAULT 0,
    total_debited BIGINT NOT NULL DEFAULT 0,
    row_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE point_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    delta BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL CHECK (transaction_type IN ('KEY_CREDIT', 'AI_GRADING_DEBIT', 'AI_GRADING_REFUND', 'ADMIN_ADJUSTMENT')),
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_point_ledger_user_id ON point_ledger_entries(user_id);
CREATE INDEX idx_point_ledger_idempotency ON point_ledger_entries(idempotency_key);
CREATE INDEX idx_point_ledger_created_at ON point_ledger_entries(created_at DESC);

-- ============================================================================
-- 5. OUTBOX EVENTS
-- ============================================================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_access_outbox_status_created ON outbox_events(status, created_at);

-- ============================================================================
-- 6. SEED DATA
-- ============================================================================

-- Default Plans
INSERT INTO plans (id, code, name, status) VALUES
    ('11111111-1111-1111-1111-111111111111', 'FREE', 'Free Plan', 'ACTIVE'),
    ('22222222-2222-2222-2222-222222222222', 'PREMIUM', 'Premium Plan', 'ACTIVE');

-- Plan Features for PREMIUM
INSERT INTO plan_features (plan_id, feature_key, is_enabled) VALUES
    ('22222222-2222-2222-2222-222222222222', 'PREMIUM_CONTENT', true),
    ('22222222-2222-2222-2222-222222222222', 'HUMAN_GRADING', true),
    ('22222222-2222-2222-2222-222222222222', 'ADVANCED_ANALYTICS', true);

-- Standard Key Products
INSERT INTO key_products (id, code, name, key_type, points_amount, plan_id, premium_days, human_grading_credits, status) VALUES
    ('33333333-3333-3333-3333-333333333331', 'POINT_50', 'Gói 50 AI Points', 'POINTS', 50, NULL, NULL, NULL, 'ACTIVE'),
    ('33333333-3333-3333-3333-333333333332', 'POINT_100', 'Gói 100 AI Points', 'POINTS', 100, NULL, NULL, NULL, 'ACTIVE'),
    ('33333333-3333-3333-3333-333333333333', 'PREMIUM_30D', 'Gói Premium 30 ngày', 'PREMIUM', NULL, '22222222-2222-2222-2222-222222222222', 30, 4, 'ACTIVE'),
    ('33333333-3333-3333-3333-333333333334', 'PREMIUM_90D', 'Gói Premium 90 ngày', 'PREMIUM', NULL, '22222222-2222-2222-2222-222222222222', 90, 12, 'ACTIVE');

