-- Demo accounts for the MVP learner flow, written after every Flyway migrate when demo data is enabled
-- (DEMO_DATA_ENABLED=true, Flyway placeholder demoData). DEV/DEMO ONLY: the password is public. Idempotent.
--   learner  00000000-0000-0000-0000-000000000001  learner@ielts.demo  Demo@123  (CUSTOMER)
--   admin    00000000-0000-0000-0000-000000000002  admin@ielts.demo    Demo@123  (ADMIN, CUSTOMER)
-- Password hash: BCrypt (cost 10) of "Demo@123", as user-service's BCryptPasswordEncoder produces.
-- An email already taken by another account is skipped, so the service still starts.

INSERT INTO users (id, email, full_name, password_hash, status)
SELECT seed.id, seed.email, seed.full_name, seed.password_hash, 'ACTIVE'
FROM (VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'learner@ielts.demo', 'Demo Learner',
              '$2a$10$/9tCVMZ4qDInCHlvrY8miOWMYJV7VlVc4XorRkPtQlJ8Lc2z2qtrG'),
             ('00000000-0000-0000-0000-000000000002'::uuid, 'admin@ielts.demo', 'Demo Admin',
              '$2a$10$/9tCVMZ4qDInCHlvrY8miOWMYJV7VlVc4XorRkPtQlJ8Lc2z2qtrG'))
     AS seed(id, email, full_name, password_hash)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'CUSTOMER'),
             ('00000000-0000-0000-0000-000000000002'::uuid, 'CUSTOMER'),
             ('00000000-0000-0000-0000-000000000002'::uuid, 'ADMIN')) AS seed(user_id, role_name)
JOIN users u ON u.id = seed.user_id
JOIN roles r ON r.name = seed.role_name
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;
