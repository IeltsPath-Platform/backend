-- Drafts an author asked the AI to produce. A draft never reaches learners; approval creates ordinary DRAFT content.
CREATE TABLE ai_drafts (
    id UUID PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('QUESTIONS', 'COURSE')),
    status VARCHAR(12) NOT NULL CHECK (status IN ('QUEUED', 'GENERATING', 'READY', 'FAILED', 'APPROVED', 'REJECTED')),
    requested_by UUID NOT NULL,
    request JSONB NOT NULL,
    result JSONB,
    error_code VARCHAR(40),
    model VARCHAR(80),
    prompt_version VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    review_note TEXT
);
CREATE INDEX idx_ai_drafts_status_created ON ai_drafts (status, created_at);
CREATE INDEX idx_ai_drafts_requester ON ai_drafts (requested_by, created_at DESC);

-- Per-author daily allowance. Kind separates generation from spreadsheet imports, which count separately.
CREATE TABLE ai_usage_daily (
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('AI', 'IMPORT')),
    count INTEGER NOT NULL CHECK (count >= 0),
    PRIMARY KEY (user_id, usage_date, kind)
);
