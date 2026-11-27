-- Avatar objects of deleted accounts waiting to be removed from storage. The row is written in the same transaction
-- as the deletion, so a storage outage delays the removal instead of losing it.
CREATE TABLE pending_avatar_deletions (
    object_key   VARCHAR(512) PRIMARY KEY,
    user_id      UUID         NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts     INT          NOT NULL DEFAULT 0,
    claimed_at   TIMESTAMPTZ,
    last_error   VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pending_avatar_deletions_status CHECK (status IN ('PENDING', 'PROCESSING', 'FAILED')),
    CONSTRAINT chk_pending_avatar_deletions_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_pending_avatar_deletions_claimable ON pending_avatar_deletions (created_at)
    WHERE status IN ('PENDING', 'PROCESSING');
