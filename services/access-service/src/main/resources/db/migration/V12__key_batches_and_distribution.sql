-- Key batches and key distribution, the data the point-management screens need (frontend features/point-management):
-- a manager creates a batch of up to 1000 keys of one product, hands keys to sales staff, a sale issues a key to a
-- student, the student activates it (the existing flow), and unused keys can be revoked.
--
-- The screens' key states are derived from the columns below plus the existing status, so ActivateKeyUseCase and the
-- status values stay as they are:
--   READY      status ACTIVE   and issued_at IS NULL        (in the shared pool when sale_id IS NULL, else with a sale)
--   ISSUED     status ACTIVE   and issued_at IS NOT NULL
--   ACTIVATED  status REDEEMED
--   REVOKED    status REVOKED  (revoked_at and note say when and why)
--   EXPIRED    status EXPIRED, or ACTIVE past expires_at
-- A key given out directly by the manager has issued_at set and sale_id NULL.
--
-- sale_id and key_batches.created_by are user ids of user-service accounts (roles SALES_STAFF and STAFF_MANAGER); they
-- are not foreign keys because the users live in another database. Keys made earlier through /keys/generate have no
-- batch: batch_code and seq stay NULL for them.

CREATE TABLE key_batches (
    -- B-YYMM-NNN, numbered per month by the service.
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    product_id UUID NOT NULL REFERENCES key_products (id) ON DELETE RESTRICT,
    quantity INT NOT NULL,
    -- The last day the keys can be activated, in Asia/Ho_Chi_Minh; NULL means no limit. Each key's expires_at is the
    -- end of that day and is what activation checks.
    expires_on DATE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_key_batches_code CHECK (code ~ '^B-[0-9]{4}-[0-9]{3}$'),
    CONSTRAINT chk_key_batches_quantity CHECK (quantity BETWEEN 1 AND 1000)
);

CREATE INDEX idx_key_batches_created_at ON key_batches (created_at DESC);
CREATE INDEX idx_key_batches_product ON key_batches (product_id);

ALTER TABLE activation_keys
    ADD COLUMN batch_code VARCHAR(20) REFERENCES key_batches (code) ON DELETE RESTRICT,
    -- Position inside the batch, from 1; keys are handed out lowest first.
    ADD COLUMN seq INT,
    ADD COLUMN sale_id UUID,
    ADD COLUMN issued_at TIMESTAMPTZ,
    ADD COLUMN note VARCHAR(500),
    ADD COLUMN revoked_at TIMESTAMPTZ,
    -- The full code, encrypted by the application, so a sale can see it again in "Chi tiết key" and export it. NULL when
    -- the application did not store it; the plain code is never kept (code_hash is what activation compares).
    ADD COLUMN code_ciphertext TEXT,
    ADD CONSTRAINT chk_activation_keys_batch_seq CHECK ((batch_code IS NULL) = (seq IS NULL)),
    ADD CONSTRAINT chk_activation_keys_seq_positive CHECK (seq IS NULL OR seq > 0),
    ADD CONSTRAINT chk_activation_keys_revoked_at CHECK (revoked_at IS NULL OR status = 'REVOKED'),
    ADD CONSTRAINT uq_activation_keys_batch_seq UNIQUE (batch_code, seq);

-- One row per allocation made by a manager: who moved how many keys of a batch, when, and between which holders. The
-- keys themselves carry the current holder (activation_keys.sale_id); this table is the history behind it. The service
-- writes a row in the same transaction that changes sale_id, so quantity matches the keys that moved.
--   ALLOCATE  from the shared pool to a sale          (from_sale_id NULL, to_sale_id set)
--   REASSIGN  from one sale to another                (both set, different)
--   WITHDRAW  from a sale back to the shared pool     (from_sale_id set, to_sale_id NULL)
-- Only keys that are still READY (not yet issued to a student) can move. from_sale_id, to_sale_id and allocated_by are
-- user-service user ids, not foreign keys.
CREATE TABLE key_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_code VARCHAR(20) NOT NULL REFERENCES key_batches (code) ON DELETE RESTRICT,
    action VARCHAR(16) NOT NULL,
    from_sale_id UUID,
    to_sale_id UUID,
    quantity INT NOT NULL,
    allocated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_key_allocations_quantity CHECK (quantity > 0),
    CONSTRAINT chk_key_allocations_holders CHECK (
        (action = 'ALLOCATE' AND from_sale_id IS NULL AND to_sale_id IS NOT NULL)
        OR (action = 'REASSIGN' AND from_sale_id IS NOT NULL AND to_sale_id IS NOT NULL AND from_sale_id <> to_sale_id)
        OR (action = 'WITHDRAW' AND from_sale_id IS NOT NULL AND to_sale_id IS NULL))
);

CREATE INDEX idx_key_allocations_batch ON key_allocations (batch_code, created_at DESC);
CREATE INDEX idx_key_allocations_to_sale ON key_allocations (to_sale_id, created_at DESC) WHERE to_sale_id IS NOT NULL;
CREATE INDEX idx_key_allocations_from_sale ON key_allocations (from_sale_id, created_at DESC) WHERE from_sale_id IS NOT NULL;

-- The key a sale hands out next for a product: soonest expiry first (no expiry last), then oldest batch, then seq.
CREATE INDEX idx_activation_keys_next_to_issue
    ON activation_keys (sale_id, product_id, expires_at NULLS LAST, batch_code, seq)
    WHERE status = 'ACTIVE' AND issued_at IS NULL;

-- A sale's issued-keys history, newest first.
CREATE INDEX idx_activation_keys_issued
    ON activation_keys (sale_id, issued_at DESC)
    WHERE issued_at IS NOT NULL;
