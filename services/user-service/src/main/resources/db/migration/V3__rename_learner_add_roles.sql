-- V3: Rename role LEARNER -> CUSTOMER and add new roles (CONTENT_AUTHOR, EXAMINER, SALES_STAFF)

-- Step 1: Drop old CHECK constraint that only allows ('ADMIN', 'LEARNER')
ALTER TABLE roles DROP CONSTRAINT chk_roles_name;

-- Step 2: Rename LEARNER -> CUSTOMER
UPDATE roles SET name = 'CUSTOMER', description = 'Customer user', updated_at = CURRENT_TIMESTAMP
WHERE name = 'LEARNER';

-- Step 3: Insert new roles
INSERT INTO roles (name, description)
VALUES
    ('CONTENT_AUTHOR', 'Content author'),
    ('EXAMINER', 'Examiner'),
    ('SALES_STAFF', 'Sales staff')
ON CONFLICT (name) DO NOTHING;

-- Step 4: Add new CHECK constraint for all 5 roles
ALTER TABLE roles ADD CONSTRAINT chk_roles_name
    CHECK (name IN ('ADMIN', 'CUSTOMER', 'CONTENT_AUTHOR', 'EXAMINER', 'SALES_STAFF'));

