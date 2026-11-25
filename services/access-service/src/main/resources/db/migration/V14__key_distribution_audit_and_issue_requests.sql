-- Key distribution: the audit trail behind the sale and manager screens, and the record that makes handing out a key
-- idempotent.
--
-- key_audit_log has one row per action that moves, shows or removes keys. It never holds a key code, only ids and counts.
--   CREATE_BATCH  a manager made a batch              (batch_code, quantity)
--   ALLOCATE      keys moved from the shared pool to a sale
--   REASSIGN      keys moved from one sale to another
--   WITHDRAW      keys moved from a sale back to the pool
--   REVOKE        unused keys taken out of use
--   ISSUE         a key was handed to a student      (key_id)
--   REVEAL        someone looked at a full code      (key_id)
--   EXPORT        a manager downloaded a batch       (batch_code, quantity)
--
-- key_issue_requests is keyed by the client's Idempotency-Key: asking again with the same key returns the same activation
-- key instead of taking another one. key_id is filled in the same transaction that takes the key.

CREATE TABLE key_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(16) NOT NULL,
    actor_id UUID NOT NULL,
    key_id UUID REFERENCES activation_keys (id) ON DELETE RESTRICT,
    batch_code VARCHAR(20) REFERENCES key_batches (code) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 1,
    detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_key_audit_log_action CHECK (action IN
        ('CREATE_BATCH', 'ALLOCATE', 'REASSIGN', 'WITHDRAW', 'REVOKE', 'ISSUE', 'REVEAL', 'EXPORT')),
    CONSTRAINT chk_key_audit_log_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_key_audit_log_key ON key_audit_log (key_id, created_at DESC) WHERE key_id IS NOT NULL;
CREATE INDEX idx_key_audit_log_actor ON key_audit_log (actor_id, created_at DESC);
CREATE INDEX idx_key_audit_log_batch ON key_audit_log (batch_code, created_at DESC) WHERE batch_code IS NOT NULL;

CREATE TABLE key_issue_requests (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    actor_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES key_products (id) ON DELETE RESTRICT,
    key_id UUID REFERENCES activation_keys (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
