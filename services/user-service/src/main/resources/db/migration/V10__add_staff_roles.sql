-- V10: add STAFF_MANAGER and STAFF to the allowed roles.
ALTER TABLE roles DROP CONSTRAINT chk_roles_name;

INSERT INTO roles (name, description)
VALUES
    ('STAFF_MANAGER', 'Staff manager'),
    ('STAFF', 'Staff')
ON CONFLICT (name) DO NOTHING;

ALTER TABLE roles ADD CONSTRAINT chk_roles_name
    CHECK (name IN ('ADMIN', 'CUSTOMER', 'CONTENT_AUTHOR', 'EXAMINER', 'SALES_STAFF', 'STAFF_MANAGER', 'STAFF'));
