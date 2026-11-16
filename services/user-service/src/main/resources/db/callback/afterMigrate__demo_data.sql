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

-- Demo learner h2 for the IELTS 5.0 demo, with the course progress, reviews, vocabulary, writing and notifications that
-- the learning, library, assessment, access and notification callbacks write for the same id. DEV/DEMO ONLY.
--   h2       00000000-0000-0000-0000-000000000003  h2@gmail.com  Demo@123  (CUSTOMER)
--   rivals   00000000-0000-0000-0000-0000000000b1 .. b9  rival1@ielts.demo .. rival9@ielts.demo  (no password; they fill
--            h2's weekly league group so the league table shows names)
INSERT INTO users (id, email, full_name, password_hash, status, email_verified_at)
SELECT seed.id, seed.email, seed.full_name, seed.password_hash, 'ACTIVE', now()
FROM (VALUES ('00000000-0000-0000-0000-000000000003'::uuid, 'h2@gmail.com', 'h2',
              '$2a$10$/9tCVMZ4qDInCHlvrY8miOWMYJV7VlVc4XorRkPtQlJ8Lc2z2qtrG'),
             ('00000000-0000-0000-0000-0000000000b1'::uuid, 'rival1@ielts.demo', 'Minh Anh', NULL),
             ('00000000-0000-0000-0000-0000000000b2'::uuid, 'rival2@ielts.demo', 'Bảo Ngọc', NULL),
             ('00000000-0000-0000-0000-0000000000b3'::uuid, 'rival3@ielts.demo', 'Quốc Huy', NULL),
             ('00000000-0000-0000-0000-0000000000b4'::uuid, 'rival4@ielts.demo', 'Thu Hà', NULL),
             ('00000000-0000-0000-0000-0000000000b5'::uuid, 'rival5@ielts.demo', 'Đức Anh', NULL),
             ('00000000-0000-0000-0000-0000000000b6'::uuid, 'rival6@ielts.demo', 'Khánh Linh', NULL),
             ('00000000-0000-0000-0000-0000000000b7'::uuid, 'rival7@ielts.demo', 'Gia Bảo', NULL),
             ('00000000-0000-0000-0000-0000000000b8'::uuid, 'rival8@ielts.demo', 'Phương Thảo', NULL),
             ('00000000-0000-0000-0000-0000000000b9'::uuid, 'rival9@ielts.demo', 'Tuấn Kiệt', NULL))
     AS seed(id, email, full_name, password_hash)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u CROSS JOIN roles r
WHERE r.name = 'CUSTOMER'
  AND u.id IN ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-0000000000b1',
               '00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-0000000000b3',
               '00000000-0000-0000-0000-0000000000b4', '00000000-0000-0000-0000-0000000000b5',
               '00000000-0000-0000-0000-0000000000b6', '00000000-0000-0000-0000-0000000000b7',
               '00000000-0000-0000-0000-0000000000b8', '00000000-0000-0000-0000-0000000000b9')
  AND '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO learner_profiles (user_id, display_name, self_reported_band)
SELECT u.id, u.full_name, CASE WHEN u.email = 'h2@gmail.com' THEN 4.5 END
FROM users u
WHERE u.id IN ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-0000000000b1',
               '00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-0000000000b3',
               '00000000-0000-0000-0000-0000000000b4', '00000000-0000-0000-0000-0000000000b5',
               '00000000-0000-0000-0000-0000000000b6', '00000000-0000-0000-0000-0000000000b7',
               '00000000-0000-0000-0000-0000000000b8', '00000000-0000-0000-0000-0000000000b9')
  AND '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

-- Demo examiner who graded h2's practice essay (assessment callback), so the examiner queue and history have a real name.
--   examiner 00000000-0000-0000-0000-000000000004  examiner@ielts.demo  Demo@123  (EXAMINER)
INSERT INTO users (id, email, full_name, password_hash, status, email_verified_at)
SELECT '00000000-0000-0000-0000-000000000004'::uuid, 'examiner@ielts.demo', 'Cô Hương (Giám khảo)',
       '$2a$10$/9tCVMZ4qDInCHlvrY8miOWMYJV7VlVc4XorRkPtQlJ8Lc2z2qtrG', 'ACTIVE', now()
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000004'::uuid, r.id
FROM roles r
WHERE r.name = 'EXAMINER' AND '${demoData}' = 'true'
  AND EXISTS (SELECT 1 FROM users u WHERE u.id = '00000000-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;
