-- V11: STAFF is not a role of this system (the staff role is SALES_STAFF); STAFF_MANAGER stays.
-- An account that already holds STAFF moves to SALES_STAFF.
INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id, sales.id
FROM user_roles ur
JOIN roles staff ON staff.id = ur.role_id AND staff.name = 'STAFF'
CROSS JOIN roles sales
WHERE sales.name = 'SALES_STAFF'
ON CONFLICT DO NOTHING;

DELETE FROM user_roles WHERE role_id IN (SELECT id FROM roles WHERE name = 'STAFF');
DELETE FROM roles WHERE name = 'STAFF';

ALTER TABLE roles DROP CONSTRAINT chk_roles_name;
ALTER TABLE roles ADD CONSTRAINT chk_roles_name
    CHECK (name IN ('ADMIN', 'CUSTOMER', 'CONTENT_AUTHOR', 'EXAMINER', 'SALES_STAFF', 'STAFF_MANAGER'));
