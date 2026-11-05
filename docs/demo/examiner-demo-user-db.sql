-- Demo examiner accounts for the examiner grading screens. DEV/DEMO ONLY: the password is the public demo password.
-- Run against user_db (idempotent). Copy the file into the container instead of piping it: a pipe through Windows
-- PowerShell re-encodes the Vietnamese text.
--   docker cp docs/demo/examiner-demo-user-db.sql ieltspath-postgres-1:/tmp/examiner-demo-user-db.sql
--   docker exec ieltspath-postgres-1 psql -U postgres -d user_db -v ON_ERROR_STOP=1 -f /tmp/examiner-demo-user-db.sql
--   00000000-0000-0000-0000-0000000000e1  examiner@ielts.demo   Demo@123  Ms Lan Phương  (EXAMINER, CUSTOMER)
--   00000000-0000-0000-0000-0000000000e2  examiner2@ielts.demo  Demo@123  Mr Minh Quân   (EXAMINER, CUSTOMER)
--   00000000-0000-0000-0000-0000000000e3  examiner3@ielts.demo  Demo@123  Ms Thu Hà      (EXAMINER, CUSTOMER)
-- Password hash: BCrypt (cost 10) of "Demo@123", the same hash as the other demo accounts.
-- The name and introduction come from learner_profiles; band and certificates from examiner_profiles. This is what a
-- learner sees when choosing an examiner. The daily limit is the default (30, Writing and Speaking together).

BEGIN;

CREATE TEMP TABLE demo_examiners (id uuid, email text, full_name text, display_name text, bio text, band numeric(2, 1),
                                  certificates jsonb) ON COMMIT DROP;
INSERT INTO demo_examiners VALUES
    ('00000000-0000-0000-0000-0000000000e1', 'examiner@ielts.demo', 'Demo Examiner', 'Ms Lan Phương',
     'Giám khảo demo: 10 năm luyện thi IELTS, chuyên Writing.', 8.5,
     '[{"name":"CELTA","issuer":"Cambridge","year":2016,"score":"A"},{"name":"IELTS Academic","issuer":"British Council","year":2023,"score":"8.5"}]'),
    ('00000000-0000-0000-0000-0000000000e2', 'examiner2@ielts.demo', 'Demo Examiner 2', 'Mr Minh Quân',
     'Giám khảo demo: cựu giám khảo IELTS, mạnh về Speaking và chữa lỗi ngữ pháp.', 8.0,
     '[{"name":"DELTA","issuer":"Cambridge","year":2019,"score":"Merit"},{"name":"IELTS Academic","issuer":"IDP","year":2022,"score":"8.0"}]'),
    ('00000000-0000-0000-0000-0000000000e3', 'examiner3@ielts.demo', 'Demo Examiner 3', 'Ms Thu Hà',
     'Giám khảo demo: 7 năm dạy Writing Task 1 và Task 2, nhận xét chi tiết từng tiêu chí.', 7.5,
     '[{"name":"TESOL","issuer":"Arizona State University","year":2018,"score":null}]');

INSERT INTO users (id, email, full_name, password_hash, status)
SELECT id, email, full_name, '$2a$10$/9tCVMZ4qDInCHlvrY8miOWMYJV7VlVc4XorRkPtQlJ8Lc2z2qtrG', 'ACTIVE' FROM demo_examiners
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT d.id, r.id FROM demo_examiners d JOIN roles r ON r.name IN ('EXAMINER', 'CUSTOMER')
ON CONFLICT DO NOTHING;

INSERT INTO learner_profiles (user_id, display_name, bio)
SELECT id, display_name, bio FROM demo_examiners
ON CONFLICT (user_id) DO UPDATE SET display_name = EXCLUDED.display_name, bio = EXCLUDED.bio, updated_at = CURRENT_TIMESTAMP;

INSERT INTO examiner_profiles (user_id, ielts_band, certificates)
SELECT id, band, certificates FROM demo_examiners
ON CONFLICT (user_id) DO UPDATE SET ielts_band = EXCLUDED.ielts_band, certificates = EXCLUDED.certificates, updated_at = CURRENT_TIMESTAMP;

SELECT u.email, lp.display_name, ep.ielts_band, ep.daily_limit, ep.accepting_submissions
FROM users u JOIN learner_profiles lp ON lp.user_id = u.id JOIN examiner_profiles ep ON ep.user_id = u.id
WHERE u.id IN (SELECT id FROM demo_examiners) ORDER BY u.email;

COMMIT;
