-- Demo learning data for the demo learner (placeholder demoUserId: h2, user-service account 00000000-0000-0000-0000-000000000003,
-- unless DEMO_LEARNER_ID names another account),
-- written after every Flyway migrate when demo data is enabled (DEMO_DATA_ENABLED=true, Flyway placeholder demoData).
-- DEV/DEMO ONLY. Idempotent: every row has a fixed id or a natural key and is skipped when it already exists, so a later
-- run never overwrites progress the demo learner has made since.
--
-- The story, relative to the time of the first run: the learner started IELTS 5.0 about 15 days ago, finished the first
-- 11 of its 26 lessons (42%) and the topic tests of the first six topics (four passed, two finished under 70%), and
-- keeps a 11-day streak. Lesson 12 is open but locked until a blocking review is done. The review list has every case:
-- blocking (practice and theory stage), optional basket, done, skipped, from a Writing criterion, from an error pattern
-- and from free practice. Knowledge points sit in all three tiers, some due for review now.
--
-- Lesson, block, knowledge point, question and package ids are the ones Content seeds for IELTS 5.0 (V105, V111, V136).

CREATE TEMP TABLE IF NOT EXISTS demo_user (id uuid);
TRUNCATE demo_user;
INSERT INTO demo_user SELECT '${demoUserId}'::uuid WHERE '${demoData}' = 'true';

CREATE TEMP TABLE IF NOT EXISTS demo_lessons (n int, lesson_id uuid, topic_id uuid, so int, skill text, kps uuid[],
    ex_block uuid, qvs uuid[], pkg uuid, pkg_ver uuid);
TRUNCATE demo_lessons;
INSERT INTO demo_lessons VALUES
    (1, 'c5030000-0000-4000-8000-000000000001'::uuid, '31000000-0000-4000-8000-000000000011'::uuid, 1, 'READING', '{c5020000-0000-4000-8000-000000000001}'::uuid[], 'c5040000-0000-4000-8000-000000000003'::uuid, '{c5070000-0000-4000-8000-000000000001,c5070000-0000-4000-8000-000000000002,c5070000-0000-4000-8000-000000000003}'::uuid[], 'c5080000-0000-4000-8000-000000000001'::uuid, 'c5090000-0000-4000-8000-000000000001'::uuid),
    (2, 'c5030000-0000-4000-8000-000000000003'::uuid, '31000000-0000-4000-8000-000000000012'::uuid, 1, 'LISTENING', '{c5020000-0000-4000-8000-000000000003}'::uuid[], 'c5040000-0000-4000-8000-000000000009'::uuid, '{c5070000-0000-4000-8000-000000000021,c5070000-0000-4000-8000-000000000022,c5070000-0000-4000-8000-000000000023}'::uuid[], 'c5080000-0000-4000-8000-000000000005'::uuid, 'c5090000-0000-4000-8000-000000000005'::uuid),
    (3, 'c5030000-0000-4000-8000-000000000005'::uuid, '31000000-0000-4000-8000-000000000013'::uuid, 1, 'WRITING', '{45000000-0000-4000-8000-020000000006,45000000-0000-4000-8000-020000000007}'::uuid[], 'c5040000-0000-4000-8000-000000000014'::uuid, '{c5070000-0000-4000-8000-000000000041,c5070000-0000-4000-8000-000000000042,c5070000-0000-4000-8000-000000000043,c5070000-0000-4000-8000-000000000044}'::uuid[], 'c5080000-0000-4000-8000-000000000009'::uuid, 'c5090000-0000-4000-8000-000000000009'::uuid),
    (4, 'c5030000-0000-4000-8000-000000000006'::uuid, '31000000-0000-4000-8000-000000000013'::uuid, 2, 'WRITING', '{45000000-0000-4000-8000-020000000003,45000000-0000-4000-8000-020000000004}'::uuid[], 'c5040000-0000-4000-8000-000000000016'::uuid, '{c5070000-0000-4000-8000-000000000051,c5070000-0000-4000-8000-000000000052,c5070000-0000-4000-8000-000000000053,c5070000-0000-4000-8000-000000000054}'::uuid[], 'c5080000-0000-4000-8000-000000000011'::uuid, 'c5090000-0000-4000-8000-000000000011'::uuid),
    (5, 'c5030000-0000-4000-8000-000000000007'::uuid, '31000000-0000-4000-8000-000000000013'::uuid, 3, 'WRITING', '{45000000-0000-4000-8000-020000000001,45000000-0000-4000-8000-020000000008}'::uuid[], 'c5040000-0000-4000-8000-000000000018'::uuid, '{c5070000-0000-4000-8000-000000000061,c5070000-0000-4000-8000-000000000062,c5070000-0000-4000-8000-000000000063,c5070000-0000-4000-8000-000000000064}'::uuid[], 'c5080000-0000-4000-8000-000000000014'::uuid, 'c5090000-0000-4000-8000-000000000014'::uuid),
    (6, 'c5030000-0000-4000-8000-000000000008'::uuid, '31000000-0000-4000-8000-000000000013'::uuid, 4, 'WRITING', '{45000000-0000-4000-8000-020000000002}'::uuid[], 'c5040000-0000-4000-8000-000000000020'::uuid, '{c5070000-0000-4000-8000-000000000071,c5070000-0000-4000-8000-000000000072,c5070000-0000-4000-8000-000000000073}'::uuid[], 'c5080000-0000-4000-8000-000000000015'::uuid, 'c5090000-0000-4000-8000-000000000015'::uuid),
    (7, 'c5030000-0000-4000-8000-000000000002'::uuid, '31000000-0000-4000-8000-000000000014'::uuid, 1, 'READING', '{c5020000-0000-4000-8000-000000000002}'::uuid[], 'c5040000-0000-4000-8000-000000000006'::uuid, '{c5070000-0000-4000-8000-000000000011,c5070000-0000-4000-8000-000000000012,c5070000-0000-4000-8000-000000000013}'::uuid[], 'c5080000-0000-4000-8000-000000000003'::uuid, 'c5090000-0000-4000-8000-000000000003'::uuid),
    (8, 'c5030000-0000-4000-8000-000000000004'::uuid, '31000000-0000-4000-8000-000000000015'::uuid, 1, 'LISTENING', '{c5020000-0000-4000-8000-000000000004}'::uuid[], 'c5040000-0000-4000-8000-000000000012'::uuid, '{c5070000-0000-4000-8000-000000000031,c5070000-0000-4000-8000-000000000032,c5070000-0000-4000-8000-000000000033}'::uuid[], 'c5080000-0000-4000-8000-000000000007'::uuid, 'c5090000-0000-4000-8000-000000000007'::uuid),
    (9, 'c5030000-0000-4000-8000-000000000009'::uuid, '31000000-0000-4000-8000-000000000016'::uuid, 1, 'WRITING', '{45000000-0000-4000-8000-020000000012,45000000-0000-4000-8000-020000000013}'::uuid[], 'c5040000-0000-4000-8000-000000000022'::uuid, '{c5070000-0000-4000-8000-000000000081,c5070000-0000-4000-8000-000000000082,c5070000-0000-4000-8000-000000000083,c5070000-0000-4000-8000-000000000084}'::uuid[], 'c5080000-0000-4000-8000-000000000018'::uuid, 'c5090000-0000-4000-8000-000000000018'::uuid),
    (10, 'c5030000-0000-4000-8000-000000000010'::uuid, '31000000-0000-4000-8000-000000000016'::uuid, 2, 'WRITING', '{45000000-0000-4000-8000-020000000009,45000000-0000-4000-8000-020000000011}'::uuid[], 'c5040000-0000-4000-8000-000000000024'::uuid, '{c5070000-0000-4000-8000-000000000091,c5070000-0000-4000-8000-000000000092,c5070000-0000-4000-8000-000000000093,c5070000-0000-4000-8000-000000000094}'::uuid[], 'c5080000-0000-4000-8000-000000000019'::uuid, 'c5090000-0000-4000-8000-000000000019'::uuid),
    (11, 'c5030000-0000-4000-8000-000000000011'::uuid, '31000000-0000-4000-8000-000000000017'::uuid, 1, 'WRITING', '{c5020000-0000-4000-8000-000000000005}'::uuid[], 'c5040000-0000-4000-8000-000000000026'::uuid, '{c5070000-0000-4000-8000-000000000105,c5070000-0000-4000-8000-000000000106,c5070000-0000-4000-8000-000000000107}'::uuid[], 'c5080000-0000-4000-8000-000000000022'::uuid, 'c5090000-0000-4000-8000-000000000022'::uuid),
    (12, 'c5030000-0000-4000-8000-000000000012'::uuid, '31000000-0000-4000-8000-000000000017'::uuid, 2, 'WRITING', '{c5020000-0000-4000-8000-000000000006}'::uuid[], 'c5040000-0000-4000-8000-000000000028'::uuid, '{c5070000-0000-4000-8000-000000000111,c5070000-0000-4000-8000-000000000112,c5070000-0000-4000-8000-000000000113}'::uuid[], 'c5080000-0000-4000-8000-000000000023'::uuid, 'c5090000-0000-4000-8000-000000000023'::uuid),
    (13, 'e2030000-0000-4000-8000-000000000001'::uuid, '4297081d-01ef-47fd-b7e2-6153e8daa80e'::uuid, 1, 'READING', '{e2020000-0000-4000-8000-000000000001}'::uuid[], 'e2040000-0000-4000-8000-000000000003'::uuid, '{e2070000-0000-4000-8000-000000000001,e2070000-0000-4000-8000-000000000002,e2070000-0000-4000-8000-000000000003}'::uuid[], 'e2080000-0000-4000-8000-000000000001'::uuid, 'e2090000-0000-4000-8000-000000000001'::uuid),
    (14, 'e2030000-0000-4000-8000-000000000002'::uuid, '4297081d-01ef-47fd-b7e2-6153e8daa80e'::uuid, 2, 'READING', '{e2020000-0000-4000-8000-000000000002}'::uuid[], 'e2040000-0000-4000-8000-000000000006'::uuid, '{e2070000-0000-4000-8000-000000000007,e2070000-0000-4000-8000-000000000008,e2070000-0000-4000-8000-000000000009}'::uuid[], 'e2080000-0000-4000-8000-000000000002'::uuid, 'e2090000-0000-4000-8000-000000000002'::uuid)
;

CREATE TEMP TABLE IF NOT EXISTS demo_topics (n int, topic_id uuid, skill text, pkg uuid, pkg_ver uuid);
TRUNCATE demo_topics;
INSERT INTO demo_topics VALUES
    (1, '31000000-0000-4000-8000-000000000011'::uuid, 'READING', 'c5080000-0000-4000-8000-000000000002'::uuid, 'c5090000-0000-4000-8000-000000000002'::uuid),
    (2, '31000000-0000-4000-8000-000000000012'::uuid, 'LISTENING', 'c5080000-0000-4000-8000-000000000006'::uuid, 'c5090000-0000-4000-8000-000000000006'::uuid),
    (3, '31000000-0000-4000-8000-000000000013'::uuid, 'WRITING', 'c5080000-0000-4000-8000-000000000016'::uuid, 'c5090000-0000-4000-8000-000000000016'::uuid),
    (4, '31000000-0000-4000-8000-000000000014'::uuid, 'READING', 'c5080000-0000-4000-8000-000000000004'::uuid, 'c5090000-0000-4000-8000-000000000004'::uuid),
    (5, '31000000-0000-4000-8000-000000000015'::uuid, 'LISTENING', 'c5080000-0000-4000-8000-000000000008'::uuid, 'c5090000-0000-4000-8000-000000000008'::uuid),
    (6, '31000000-0000-4000-8000-000000000016'::uuid, 'WRITING', 'c5080000-0000-4000-8000-000000000021'::uuid, 'c5090000-0000-4000-8000-000000000021'::uuid),
    (7, '31000000-0000-4000-8000-000000000017'::uuid, 'WRITING', 'c5080000-0000-4000-8000-000000000024'::uuid, 'c5090000-0000-4000-8000-000000000024'::uuid),
    (8, '4297081d-01ef-47fd-b7e2-6153e8daa80e'::uuid, 'READING', 'e2080000-0000-4000-8000-000000000003'::uuid, 'e2090000-0000-4000-8000-000000000003'::uuid)
;

-- ---------------------------------------------------------------------------------------------------------------------
-- Course start and lesson progress: lessons 1-11 done (one hour apart in the evening, spread over two weeks), 12 opened.
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO course_study_activity (user_id, course_id, started_at, last_studied_at)
SELECT u.id, '30000000-0000-4000-8000-000000000006'::uuid, now() - interval '15 days', now() - interval '2 hours'
FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids, passed_block_ids,
                             completed_at, updated_at)
SELECT u.id, l.lesson_id, l.topic_id, l.so, l.kps,
       CASE WHEN l.n <= 11 THEN ARRAY[l.ex_block::text] ELSE '{}'::text[] END,
       CASE WHEN l.n <= 11 THEN now() - (12 - l.n) * interval '30 hours' END,
       CASE WHEN l.n <= 11 THEN now() - (12 - l.n) * interval '30 hours' ELSE now() - interval '3 hours' END
FROM demo_user u CROSS JOIN demo_lessons l
WHERE l.n <= 12
ON CONFLICT DO NOTHING;

-- One passed submission per finished lesson; lessons 3 and 9 also keep a failed first try.
INSERT INTO lesson_exercise_submissions (id, user_id, lesson_id, block_id, request_id, answers, block_passed, response,
                                         submitted_at)
SELECT md5('${demoUserId}:demo:lex:' || l.n || ':' || t.try)::uuid, u.id, l.lesson_id, l.ex_block,
       md5('${demoUserId}:demo:lex-request:' || l.n || ':' || t.try)::uuid, '[]'::jsonb, t.passed, '{}'::jsonb,
       now() - (12 - l.n) * interval '30 hours' - t.minutes * interval '1 minute'
FROM demo_user u
CROSS JOIN demo_lessons l
CROSS JOIN (VALUES (1, true, 10), (0, false, 55)) AS t(try, passed, minutes)
WHERE l.n <= 11 AND (t.passed OR l.n IN (3, 9))
ON CONFLICT DO NOTHING;

-- Practice: every finished lesson but 4 passed on the first submission; 4 needed a review, 9 attempted every set.
INSERT INTO lesson_practice_passes (user_id, lesson_id, reason, passed_at)
SELECT u.id, l.lesson_id,
       CASE l.n WHEN 4 THEN 'REVIEW_FINISHED' WHEN 9 THEN 'ALL_SETS_ATTEMPTED' ELSE 'FIRST_SUBMISSION' END,
       now() - (12 - l.n) * interval '30 hours' + interval '25 minutes'
FROM demo_user u CROSS JOIN demo_lessons l
WHERE l.n <= 10
ON CONFLICT DO NOTHING;

INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, started_at, submitted_at,
                               request_id, correct_count, total_count, passed, counted_as_evidence, response, skills,
                               passed_skills, origin, knowledge_point_id)
SELECT md5('${demoUserId}:demo:practice:' || l.n || ':' || a.try)::uuid, u.id, l.lesson_id, l.skill, l.pkg, l.pkg_ver,
       now() - (12 - l.n) * interval '30 hours' + interval '5 minutes' + a.try * interval '12 minutes',
       now() - (12 - l.n) * interval '30 hours' + interval '15 minutes' + a.try * interval '12 minutes',
       md5('${demoUserId}:demo:practice-request:' || l.n || ':' || a.try)::uuid,
       CASE WHEN a.passed THEN 4 ELSE 2 END, 5, a.passed, a.passed, '{}'::jsonb,
       ARRAY[l.skill]::varchar[], CASE WHEN a.passed THEN ARRAY[l.skill]::varchar[] ELSE '{}'::varchar[] END,
       'LESSON', NULL
FROM demo_user u
CROSS JOIN demo_lessons l
CROSS JOIN (VALUES (0, false), (1, true)) AS a(try, passed)
WHERE l.n <= 10 AND (a.passed OR l.n IN (4, 9))
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Topics: tests taken for the first six topics (first score decides: 70% and up passes, below only finishes the topic).
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO topic_progress (user_id, topic_id, sequence_order, passed_at, updated_at, skill, has_topic_test, course_id,
                            skills, tested_at, first_test_percent)
SELECT u.id, t.topic_id, t.n,
       CASE WHEN s.percent >= 70 THEN now() - (11 - t.n * 1.6) * interval '30 hours' + interval '40 minutes' END,
       now() - (11 - t.n * 1.6) * interval '30 hours' + interval '40 minutes', t.skill, true,
       '30000000-0000-4000-8000-000000000006'::uuid, ARRAY[t.skill]::varchar[],
       CASE WHEN s.percent IS NOT NULL THEN now() - (11 - t.n * 1.6) * interval '30 hours' + interval '40 minutes' END,
       s.percent
FROM demo_user u
CROSS JOIN demo_topics t
LEFT JOIN (VALUES (1, 85.0), (2, 72.0), (3, 64.0), (4, 90.0), (5, 78.0), (6, 55.0)) AS s(n, percent) ON s.n = t.n
WHERE t.n <= 7
ON CONFLICT DO NOTHING;

INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at, consumed_attempt_id,
                                    consumed_at, percent)
SELECT md5('${demoUserId}:demo:topic-test:' || t.n)::uuid, u.id, t.topic_id, t.pkg, t.pkg_ver,
       now() - (11 - t.n * 1.6) * interval '30 hours', md5('${demoUserId}:demo:topic-test-attempt:' || t.n)::uuid,
       now() - (11 - t.n * 1.6) * interval '30 hours' + interval '40 minutes', s.percent
FROM demo_user u
CROSS JOIN demo_topics t
JOIN (VALUES (1, 85.0), (2, 72.0), (3, 64.0), (4, 90.0), (5, 78.0), (6, 55.0)) AS s(n, percent) ON s.n = t.n
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Knowledge points of the finished lessons: five answers each, mastery in all three tiers.
--   rank % 6 = 0 -> PRIORITY, 1 or 3 -> CONSOLIDATE, otherwise MAINTAIN
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TEMP TABLE IF NOT EXISTS demo_kps (kp uuid, rank int, first_lesson int, tier text);
TRUNCATE demo_kps;
INSERT INTO demo_kps
SELECT k.kp, (row_number() OVER (ORDER BY min(l.n), k.kp))::int, min(l.n),
       CASE (row_number() OVER (ORDER BY min(l.n), k.kp) % 6) WHEN 0 THEN 'PRIORITY' WHEN 1 THEN 'CONSOLIDATE'
            WHEN 3 THEN 'CONSOLIDATE' ELSE 'MAINTAIN' END
FROM demo_lessons l CROSS JOIN LATERAL unnest(l.kps) AS k(kp)
WHERE l.n <= 11
GROUP BY k.kp;

CREATE TEMP TABLE IF NOT EXISTS demo_answers (kp uuid, tier text, k int, lesson_n int, qv uuid, correct boolean,
    at timestamptz, error_code text);
TRUNCATE demo_answers;
INSERT INTO demo_answers
SELECT d.kp, d.tier, s.k, l.n, l.qvs[1 + ((d.rank + s.k) % cardinality(l.qvs))],
       CASE d.tier WHEN 'PRIORITY' THEN s.k IN (2, 5) WHEN 'CONSOLIDATE' THEN s.k IN (1, 3, 4) ELSE s.k <> 4 END,
       now() - (12 - l.n) * interval '30 hours' - (6 - s.k) * interval '9 minutes',
       (ARRAY['ARTICLE', 'COLLOCATION', 'COMPARISON', 'GRAMMAR_OTHER'])[1 + (d.rank % 4)]
FROM demo_kps d
JOIN demo_lessons l ON l.n = d.first_lesson
CROSS JOIN generate_series(1, 5) AS s(k);

INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, attempt_id, result_version, created_at,
                         question_version_id)
SELECT md5('${demoUserId}:demo:evidence:' || a.kp || ':' || a.k)::uuid, u.id, a.kp, a.correct,
       CASE WHEN a.k <= 3 THEN 'lesson_exercise' ELSE 'practice_set' END,
       md5('${demoUserId}:demo:event-source:' || a.lesson_n || ':' || a.k || ':' || a.kp)::uuid, NULL, NULL, a.at, a.qv
FROM demo_user u CROSS JOIN demo_answers a
ON CONFLICT DO NOTHING;

INSERT INTO question_attempt_events (id, user_id, question_version_id, knowledge_point_ids, correct, score, max_score,
                                     duration_ms, error_code, source, source_reference_id, attempt_id, first_submission,
                                     occurred_at)
SELECT md5('${demoUserId}:demo:event:' || a.kp || ':' || a.k)::uuid, u.id, a.qv, ARRAY[a.kp], a.correct,
       CASE WHEN a.correct THEN 1 ELSE 0 END, 1, 14000 + (a.k * 3500) + (a.lesson_n * 700),
       CASE WHEN a.correct THEN NULL ELSE a.error_code END,
       CASE WHEN a.k <= 3 THEN 'lesson_exercise' ELSE 'practice_set' END,
       md5('${demoUserId}:demo:event-source:' || a.lesson_n || ':' || a.k || ':' || a.kp)::uuid, NULL, true, a.at
FROM demo_user u CROSS JOIN demo_answers a
ON CONFLICT DO NOTHING;

INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, success_streak,
                                 last_evidence_at, next_review_at, median_duration_ms, top_error_codes, updated_at,
                                 confidence)
SELECT u.id, d.kp,
       CASE d.tier WHEN 'PRIORITY' THEN 0.30 + (d.rank % 5) * 0.02
                   WHEN 'CONSOLIDATE' THEN 0.56 + (d.rank % 5) * 0.025 ELSE 0.82 + (d.rank % 5) * 0.03 END,
       d.tier, 5,
       CASE d.tier WHEN 'PRIORITY' THEN 0 WHEN 'CONSOLIDATE' THEN 1 ELSE 3 END,
       (SELECT max(a.at) FROM demo_answers a WHERE a.kp = d.kp),
       CASE d.tier WHEN 'PRIORITY' THEN now() - interval '1 day'
                   WHEN 'CONSOLIDATE' THEN CASE WHEN d.rank % 2 = 0 THEN now() - interval '3 hours'
                                                ELSE now() + interval '2 days' END
                   ELSE now() + interval '6 days' END,
       19000 + d.rank * 400,
       CASE d.tier WHEN 'MAINTAIN' THEN '{}'::varchar[]
                   ELSE ARRAY[(ARRAY['ARTICLE', 'COLLOCATION', 'COMPARISON', 'GRAMMAR_OTHER'])[1 + (d.rank % 4)]] END,
       now() - interval '2 hours', 0.6 + (d.rank % 4) * 0.1
FROM demo_user u CROSS JOIN demo_kps d
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Reviews: one of every case the review list can show.
--   blocking, practice stage (lesson 11)         blocking, theory stage (lesson 10)
--   optional basket from consolidation (3), maintenance (8), free practice (1), an error pattern (6), a Writing criterion (11)
--   done (lesson 2, with its passed set)         skipped (lesson 7, no unseen set left)
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, created_at, done_at, skill, trigger_kind,
                          source_attempt_id, stage, theory_reason, theory_completed_count, blocking)
SELECT md5('${demoUserId}:demo:review:' || r.code)::uuid, u.id,
       (SELECT d.kp FROM demo_kps d WHERE d.first_lesson = r.lesson_n ORDER BY d.kp LIMIT 1),
       l.lesson_id, r.status, now() - r.age, r.done_at, l.skill, r.trigger_kind, NULL, r.stage, r.theory_reason,
       r.theory_done, r.blocking
FROM demo_user u
CROSS JOIN (VALUES
    ('blocking-practice', 11, 'PENDING', interval '5 hours', NULL::timestamptz, 'PRACTICE', 'PRACTICE', NULL, 0, true),
    ('blocking-theory', 10, 'PENDING', interval '1 day', NULL, 'PRACTICE', 'THEORY', 'SECOND_FAIL', 1, true),
    ('basket-consolidate', 3, 'PENDING', interval '3 days', NULL, 'CONSOLIDATE', 'PRACTICE', NULL, 0, false),
    ('basket-maintain', 8, 'PENDING', interval '2 days', NULL, 'MAINTAIN', 'PRACTICE', NULL, 0, false),
    ('basket-free-practice', 1, 'PENDING', interval '6 days', NULL, 'FREE_PRACTICE_TEST', 'PRACTICE', NULL, 0, false),
    ('basket-error-pattern', 6, 'PENDING', interval '4 days', NULL, 'ERROR_PATTERN', 'PRACTICE', NULL, 0, false),
    ('done', 2, 'DONE', interval '11 days', now() - interval '9 days', 'PRACTICE', 'PRACTICE', NULL, 0, true),
    ('skipped', 7, 'SKIPPED', interval '8 days', now() - interval '7 days', 'PRACTICE', 'PRACTICE', NULL, 0, true)
) AS r(code, lesson_n, status, age, done_at, trigger_kind, stage, theory_reason, theory_done, blocking)
JOIN demo_lessons l ON l.n = r.lesson_n
ON CONFLICT DO NOTHING;

-- The Writing criterion review: the criterion's knowledge point is the one of lesson 11, the practised point is the most
-- frequent error's (a grammar point of lesson 3).
INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, created_at, skill, trigger_kind, stage,
                          theory_completed_count, blocking, criterion_knowledge_point_id, source_submission_id)
SELECT md5('${demoUserId}:demo:review:basket-writing-criterion')::uuid, u.id,
       (SELECT d.kp FROM demo_kps d WHERE d.first_lesson = 3 ORDER BY d.kp OFFSET 1 LIMIT 1), l.lesson_id, 'PENDING',
       now() - interval '20 hours', 'WRITING', 'WRITING_CRITERION', 'PRACTICE', 0, false, l.kps[1], md5('${demoUserId}:demo:essay:task2-first')::uuid
FROM demo_user u JOIN demo_lessons l ON l.n = 11
ON CONFLICT DO NOTHING;

-- The set of the open blocking practice review is assigned and untouched; the done review's set was passed.
INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, assigned_at, submitted_at, passed,
                         request_id, response, correct_count, total_count)
SELECT md5('${demoUserId}:demo:review-set:' || s.code)::uuid, md5('${demoUserId}:demo:review:' || s.code)::uuid, u.id, l.pkg, l.pkg_ver,
       now() - s.assigned, s.submitted_at, s.passed, s.request_id, s.response, s.correct_count, s.total_count
FROM demo_user u
CROSS JOIN (VALUES
    ('blocking-practice', 11, interval '4 hours', NULL::timestamptz, NULL::boolean, NULL::uuid, NULL::jsonb, NULL::int, NULL::int),
    ('done', 2, interval '10 days', now() - interval '9 days', true, md5('${demoUserId}:demo:review-set-request:done')::uuid, '{}'::jsonb, 4, 5)
) AS s(code, lesson_n, assigned, submitted_at, passed, request_id, response, correct_count, total_count)
JOIN demo_lessons l ON l.n = s.lesson_n
WHERE EXISTS (SELECT 1 FROM review_items i WHERE i.id = md5('${demoUserId}:demo:review:' || s.code)::uuid)
ON CONFLICT DO NOTHING;

-- The theory check of the blocking theory review: one of two checks passed, showing partial progress.
INSERT INTO review_theory_checks (id, review_item_id, user_id, request_id, question_version_ids, answers, correct_count,
                                  total_count, response, submitted_at)
SELECT md5('${demoUserId}:demo:theory-check:1')::uuid, md5('${demoUserId}:demo:review:blocking-theory')::uuid, u.id,
       md5('${demoUserId}:demo:theory-check-request:1')::uuid, l.qvs[1:2], '[]'::jsonb, 1, 2, '{}'::jsonb, now() - interval '20 hours'
FROM demo_user u JOIN demo_lessons l ON l.n = 10
WHERE EXISTS (SELECT 1 FROM review_items i WHERE i.id = md5('${demoUserId}:demo:review:blocking-theory')::uuid)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Settings, goal, placement
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO learner_settings (user_id, timezone, updated_at)
SELECT u.id, 'Asia/Ho_Chi_Minh', now() FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO learning_goals (id, user_id, target_band, exam_date, available_minutes_per_day, status, started_at, created_at,
                            updated_at)
SELECT md5('${demoUserId}:demo:goal')::uuid, u.id, 6.0, (now() + interval '75 days')::date, 45, 'ACTIVE',
       (now() - interval '15 days')::timestamp, (now() - interval '15 days')::timestamp, (now() - interval '15 days')::timestamp
FROM demo_user u
WHERE NOT EXISTS (SELECT 1 FROM learning_goals g WHERE g.user_id = u.id)
ON CONFLICT DO NOTHING;

INSERT INTO learner_placements (user_id, band, attempt_id, completed_at, updated_at)
SELECT u.id, 4.5, md5('${demoUserId}:demo:placement-attempt')::uuid, now() - interval '16 days', now() - interval '16 days'
FROM demo_user u
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- XP, streak, hearts, quests, badges and the weekly league
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO xp_ledger (id, user_id, amount, source, source_ref, local_date, counted, created_at)
SELECT md5('${demoUserId}:demo:xp:day:' || d.day)::uuid, u.id, 40 + ((d.day * 37) % 70), 'QUESTION', 'demo:day:' || d.day,
       ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - d.day), true, now() - d.day * interval '1 day' - interval '90 minutes'
FROM demo_user u CROSS JOIN generate_series(0, 10) AS d(day)
ON CONFLICT DO NOTHING;

INSERT INTO xp_ledger (id, user_id, amount, source, source_ref, local_date, counted, created_at)
SELECT md5('${demoUserId}:demo:xp:' || x.ref)::uuid, u.id, x.amount, x.source, x.ref,
       ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - x.day), true, now() - x.day * interval '1 day' - interval '60 minutes'
FROM demo_user u
CROSS JOIN (VALUES ('STREAK', 'streak:7', 50, 4), ('QUEST', 'quest:practice:today', 30, 0),
                   ('KP_CONSOLIDATE', 'kp:consolidate:1', 25, 3), ('KP_MAINTAIN', 'kp:maintain:1', 40, 5)) AS x(source, ref, amount, day)
ON CONFLICT DO NOTHING;

INSERT INTO learner_xp (user_id, total_xp, level, updated_at)
SELECT u.id, COALESCE(sum(x.amount), 0), COALESCE(sum(x.amount), 0) / 500 + 1, now()
FROM demo_user u LEFT JOIN xp_ledger x ON x.user_id = u.id AND x.counted
GROUP BY u.id
ON CONFLICT DO NOTHING;

INSERT INTO learner_streaks (user_id, current_days, longest_days, last_active_date, streak_start, evaluated_through,
                             freeze_available, updated_at)
SELECT u.id, 11, 11, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 10,
       (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, true, now()
FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO learner_hearts (user_id, hearts, max_hearts, last_regen_at, updated_at)
SELECT u.id, 3, 7, now() - interval '2 hours', now() FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO daily_quests (id, user_id, local_date, type, target_kp_id, required, progress, completed_at, xp_reward,
                          points_reward)
SELECT md5('${demoUserId}:demo:quest:' || q.type)::uuid, u.id, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, q.type,
       CASE q.type WHEN 'WEAKNESS' THEN (SELECT d.kp FROM demo_kps d WHERE d.tier = 'PRIORITY' ORDER BY d.kp LIMIT 1) END,
       q.required, q.progress, CASE WHEN q.progress >= q.required THEN now() - interval '3 hours' END, q.xp, q.points
FROM demo_user u
CROSS JOIN (VALUES ('PRACTICE', 5, 5, 30, 2), ('WEAKNESS', 1, 0, 40, 3), ('STREAK', 1, 1, 20, 1)) AS q(type, required, progress, xp, points)
ON CONFLICT DO NOTHING;

INSERT INTO learner_badges (user_id, badge_code, awarded_at)
SELECT u.id, b.code, now() - b.age
FROM demo_user u
CROSS JOIN (VALUES ('FIRST_TOPIC_TEST', interval '13 days'), ('STREAK_7', interval '4 days')) AS b(code, age)
WHERE EXISTS (SELECT 1 FROM badges x WHERE x.code = b.code)
ON CONFLICT DO NOTHING;

-- League: silver tier this week, fifth of ten. The nine rivals are the demo rivals user-service creates (...00b1 to ...00b9).
INSERT INTO learner_league (user_id, tier, updated_at)
SELECT u.id, 2, now() FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO league_weeks (week_start, closed_at)
SELECT date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, NULL
WHERE EXISTS (SELECT 1 FROM demo_user)
ON CONFLICT DO NOTHING;

INSERT INTO league_groups (id, week_start, tier, created_at)
SELECT md5('${demoUserId}:demo:league-group')::uuid, date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, 2, now() - interval '3 days'
WHERE EXISTS (SELECT 1 FROM demo_user)
ON CONFLICT DO NOTHING;

INSERT INTO league_members (week_start, user_id, group_id, tier, weekly_xp, xp_updated_at)
SELECT date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, m.user_id, md5('${demoUserId}:demo:league-group')::uuid, 2,
       m.weekly_xp, now() - interval '1 hour'
FROM demo_user u
CROSS JOIN LATERAL (VALUES (u.id, 310), ('00000000-0000-0000-0000-0000000000b1'::uuid, 520),
    ('00000000-0000-0000-0000-0000000000b2'::uuid, 480), ('00000000-0000-0000-0000-0000000000b3'::uuid, 395),
    ('00000000-0000-0000-0000-0000000000b4'::uuid, 280), ('00000000-0000-0000-0000-0000000000b5'::uuid, 240),
    ('00000000-0000-0000-0000-0000000000b6'::uuid, 190), ('00000000-0000-0000-0000-0000000000b7'::uuid, 150),
    ('00000000-0000-0000-0000-0000000000b8'::uuid, 95), ('00000000-0000-0000-0000-0000000000b9'::uuid, 40)) AS m(user_id, weekly_xp)
WHERE EXISTS (SELECT 1 FROM league_groups g WHERE g.id = md5('${demoUserId}:demo:league-group')::uuid)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Study time (the last eleven days, 25 to 70 minutes) and two weekly snapshots for the "your week" report
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO study_daily_totals (user_id, local_date, timezone, scope_key, active_ms)
SELECT u.id, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - d.day, 'Asia/Ho_Chi_Minh', 'ALL',
       (25 + ((d.day * 17) % 46)) * 60000::bigint
FROM demo_user u CROSS JOIN generate_series(0, 10) AS d(day)
ON CONFLICT DO NOTHING;

INSERT INTO learner_weekly_snapshots (user_id, week_start, skills, tier_counts, answered, correct, active_minutes,
                                      focus_kp_ids, estimated_band, created_at)
SELECT u.id, w.week_start,
       jsonb_build_array(
           jsonb_build_object('skill', 'READING', 'mastery', w.reading, 'kpCount', 3,
                              'tierCounts', jsonb_build_object('PRIORITY', 0, 'CONSOLIDATE', 1, 'MAINTAIN', 2, 'INSUFFICIENT', 0)),
           jsonb_build_object('skill', 'LISTENING', 'mastery', w.listening, 'kpCount', 3,
                              'tierCounts', jsonb_build_object('PRIORITY', 0, 'CONSOLIDATE', 1, 'MAINTAIN', 2, 'INSUFFICIENT', 0)),
           jsonb_build_object('skill', 'GRAMMAR', 'mastery', w.grammar, 'kpCount', 6,
                              'tierCounts', jsonb_build_object('PRIORITY', 1, 'CONSOLIDATE', 2, 'MAINTAIN', 3, 'INSUFFICIENT', 0))),
       jsonb_build_object('PRIORITY', w.priority, 'CONSOLIDATE', 4, 'MAINTAIN', w.maintain, 'INSUFFICIENT', 3),
       w.answered, w.correct, w.minutes,
       (SELECT array_agg(f.kp) FROM (SELECT d.kp FROM demo_kps d WHERE d.tier <> 'MAINTAIN' ORDER BY d.rank LIMIT 3) f),
       w.band, now()
FROM demo_user u
CROSS JOIN (VALUES
    ((date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 14), 0.52, 0.48, 0.41, 2, 3, 38, 27, 160, 4.5),
    ((date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 7), 0.66, 0.61, 0.55, 2, 6, 64, 49, 240, 5.0)
) AS w(week_start, reading, listening, grammar, priority, maintain, answered, correct, minutes, band)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Writing graded by AI (free writing): a Task 2 essay that fell short of band 6, the revision that passed, and a Task 1
-- essay. Prompts are the published demo prompts of Content (V10 green roofs, V11 roof temperatures).
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id, knowledge_point_ids,
                                        request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                                        debit_ledger_entry_id, result, overall_band, passed, grading_started_at,
                                        submitted_at, graded_at, source, practice_attempt_id, prompt_version,
                                        previous_submission_id, revision_no, essay_hash)
SELECT md5('${demoUserId}:demo:essay:' || e.code)::uuid, u.id, NULL, NULL, e.question_version_id, ARRAY[e.kp]::uuid[],
       md5('${demoUserId}:demo:essay-request:' || e.code)::uuid, e.text,
       cardinality(regexp_split_to_array(trim(e.text), '\s+')), e.prompt::jsonb, 'GRADED', 20,
       md5('${demoUserId}:demo:ledger:2')::uuid, e.result::jsonb, e.band, e.band >= 6.0,
       now() - e.age, now() - e.age, now() - e.age + interval '40 seconds', 'FREE_WRITING', NULL,
       'essay-prompt-v3+tplv3',
       CASE WHEN e.code = 'task2-revised' THEN md5('${demoUserId}:demo:essay:task2-first')::uuid END,
       CASE WHEN e.code = 'task2-revised' THEN 2 ELSE 1 END, md5(e.text) || md5(reverse(e.text))
FROM demo_user u
CROSS JOIN (VALUES
('task2-first', '21000000-0000-4000-8000-070000000001'::uuid, '21000000-0000-4000-8000-020000000007'::uuid, 5.0, interval '9 days',
$essay$In my opinion, green roofs is a good idea but I don't think all new buildings must have it.

First of all, green roofs have many advantage. They make the building cooler in summer so people need less air conditioner. Also, plants on the roof can absorb the rain water and it help the city to avoid flood. For example, in Singapore many building have gardens on the roof and the city look very beautiful.

However, there are some problem if the government force every builder to do this. Green roof is expensive to build and the owner need to take care of the plants every week. A small family which build a small house maybe cannot pay for this. Moreover, some old roofs is not strong enough, so it can be dangerous.

In my view, the government should require green roofs only for big buildings like shopping malls or office, because they have more money and the effect is bigger. For small houses, the government can give some money to help people who want to do it.

To conclude, green roofs are useful but it should not be compulsory for all buildings.$essay$,
$prompt$
{"questionVersionId":"21000000-0000-4000-8000-070000000001","stem":"Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.","task":"TASK_2","minWords":250,"passBand":6,"chartFacts":null,"sampleAnswer":null,"images":[],"knowledgePointIds":["21000000-0000-4000-8000-020000000007"]}$prompt$,
$result$
{"criteria":[
 {"code":"TR","band":5.0,"strengths":["Nêu rõ quan điểm ở đầu bài và nhắc lại ở kết bài.","Có ví dụ cụ thể (Singapore)."],"improvements":["Bài chưa đủ 250 từ nên chưa phát triển đủ ý.","Mỗi lý do cần giải thích sâu hơn thay vì chỉ nêu."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Trả lời đúng đề nhưng phát triển ý còn hạn chế.","nextBand":{"band":5.5,"descriptor":"Phát triển ý đầy đủ hơn và đủ độ dài yêu cầu."}},
 {"code":"CC","band":5.5,"strengths":["Bố cục bốn đoạn rõ ràng."],"improvements":["Dùng thêm từ nối giữa các ý trong đoạn thân bài."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Bố cục rõ nhưng liên kết còn đơn giản.","nextBand":{"band":6.0,"descriptor":"Dùng đa dạng từ nối và tham chiếu."}},
 {"code":"LR","band":5.0,"strengths":["Từ vựng đủ để diễn đạt ý chính."],"improvements":["Lặp từ green roof, building nhiều lần; thêm từ đồng nghĩa.","Sai kết hợp từ: less air conditioner."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Vốn từ hạn chế, có lỗi kết hợp từ.","nextBand":{"band":5.5,"descriptor":"Dùng từ chính xác hơn và ít lặp."}},
 {"code":"GRA","band":4.5,"strengths":["Một số câu phức dùng đúng."],"improvements":["Lỗi hòa hợp chủ ngữ và động từ lặp lại nhiều lần.","Thiếu -s ở danh từ số nhiều."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Nhiều lỗi ngữ pháp, một số làm người đọc phải đoán ý.","nextBand":{"band":5.0,"descriptor":"Giảm lỗi cơ bản về chia động từ và số nhiều."}}],
 "corrections":[
 {"excerpt":"green roofs is a good idea","suggestion":"green roofs are a good idea","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Hòa hợp chủ ngữ và động từ","fixHint":"Chủ ngữ số nhiều đi với động từ số nhiều: roofs are.","severity":2,"reason":"Chủ ngữ số nhiều nhưng dùng động từ số ít.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"many advantage","suggestion":"many advantages","category":"GRAMMAR","errorCode":"PLURAL_FORM","errorName":"Sai dạng số nhiều","fixHint":"Sau many dùng danh từ đếm được số nhiều.","severity":2,"reason":"Thiếu -s sau many.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"less air conditioner","suggestion":"less air conditioning","category":"VOCABULARY","errorCode":"COLLOCATION","errorName":"Kết hợp từ không tự nhiên","fixHint":"Dùng air conditioning (không đếm được) sau less.","severity":2,"reason":"Air conditioner là danh từ đếm được.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"it help the city","suggestion":"it helps the city","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Hòa hợp chủ ngữ và động từ","fixHint":"Chủ ngữ it đi với động từ thêm -s.","severity":2,"reason":"Thiếu -s ở động từ ngôi thứ ba số ít.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"many building have gardens","suggestion":"many buildings have gardens","category":"GRAMMAR","errorCode":"PLURAL_FORM","errorName":"Sai dạng số nhiều","fixHint":"Sau many dùng danh từ số nhiều.","severity":2,"reason":"Thiếu -s sau many.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"Green roof is expensive","suggestion":"A green roof is expensive","category":"GRAMMAR","errorCode":"ARTICLE","errorName":"Sai mạo từ","fixHint":"Danh từ đếm được số ít cần mạo từ.","severity":2,"reason":"Thiếu mạo từ trước danh từ số ít.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"the owner need to take care","suggestion":"the owner needs to take care","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Hòa hợp chủ ngữ và động từ","fixHint":"Chủ ngữ số ít đi với needs.","severity":2,"reason":"Thiếu -s ở động từ ngôi thứ ba số ít.","certainty":"ERROR","impedesMeaning":false}],
 "summary":"Bài có quan điểm rõ và bố cục tốt, nhưng còn ngắn và lặp lại lỗi hòa hợp chủ ngữ - động từ cùng lỗi số nhiều. Sửa hai nhóm lỗi này và phát triển thêm mỗi lý do sẽ giúp bạn lên band 6.",
 "studyPoints":[{"kind":"CRITERION","criterionCode":"GRA","band":4.5,"code":"GRA","name":"Ngữ pháp","title":"Ngữ pháp","reason":"Lỗi hòa hợp chủ ngữ và động từ lặp lại nhiều lần."},{"kind":"AI","title":"Phát triển ý","reason":"Giải thích sâu hơn mỗi lý do để đạt đủ độ dài."}],
 "judgmentVersion":"kpj3","model":"demo"}$result$),
('task2-revised', '21000000-0000-4000-8000-070000000001'::uuid, '21000000-0000-4000-8000-020000000007'::uuid, 6.0, interval '8 days',
$essay$Many cities are getting hotter and suffering more floods, so some people argue that every new building should have a green roof. I agree that green roofs are valuable, but I do not think the rule should apply to every building.

There are clear advantages to green roofs. First, the plants and soil keep the rooms below cooler, which means people need less air conditioning during heatwaves. Second, the soil absorbs rainwater and releases it slowly, so the drains are less likely to overflow. In Singapore, for example, many buildings have rooftop gardens, and the city is both cooler and more attractive as a result.

However, a single rule for all buildings ignores important differences. A green roof is expensive to install, and the owner has to water and inspect the plants for many years. A family building a small house may not be able to afford this. In addition, an older roof may not be strong enough to carry the extra weight, which could make the building unsafe.

A better approach would be to target the requirement. Large residential and commercial buildings, where the benefits are greatest and the owners have more money, could be obliged to include green roofs. Smaller projects could instead receive tax reductions or grants if their owners choose to build one.

In conclusion, green roofs deserve support, but making them compulsory for every new building would be unfair and sometimes dangerous. A targeted rule combined with incentives would bring most of the benefits at a much lower cost.$essay$,
$prompt$
{"questionVersionId":"21000000-0000-4000-8000-070000000001","stem":"Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.","task":"TASK_2","minWords":250,"passBand":6,"chartFacts":null,"sampleAnswer":null,"images":[],"knowledgePointIds":["21000000-0000-4000-8000-020000000007"]}$prompt$,
$result$
{"criteria":[
 {"code":"TR","band":6.0,"strengths":["Trả lời đủ các phần của đề với quan điểm nhất quán.","Có đề xuất giải pháp cụ thể ở đoạn bốn."],"improvements":["Ví dụ Singapore có thể nêu thêm số liệu hoặc kết quả."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Trả lời đầy đủ, ý được phát triển khá tốt.","nextBand":{"band":6.5,"descriptor":"Phát triển ý sâu hơn bằng ví dụ có chi tiết."}},
 {"code":"CC","band":6.0,"strengths":["Từ nối First, Second, However, In addition dùng đúng chỗ.","Mỗi đoạn có câu chủ đề rõ."],"improvements":["Thử đa dạng cách nối ngoài các từ nối cơ bản."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Bố cục và liên kết tốt.","nextBand":{"band":6.5,"descriptor":"Liên kết tự nhiên hơn, ít dùng từ nối máy móc."}},
 {"code":"LR","band":6.0,"strengths":["Dùng từ đúng ngữ cảnh: obliged, tax reductions, heatwaves."],"improvements":["Tránh lặp lại cụm green roofs; dùng rooftop gardens hoặc planted roofs."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Vốn từ đủ rộng và chính xác.","nextBand":{"band":6.5,"descriptor":"Dùng từ ít phổ biến hơn một cách chính xác."}},
 {"code":"GRA","band":5.5,"strengths":["Nhiều câu phức đúng cấu trúc.","Đã sửa hết lỗi hòa hợp và số nhiều của bản đầu."],"improvements":["Còn vài lỗi nhỏ về mạo từ và giới từ."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Ngữ pháp khá đa dạng, ít lỗi làm sai nghĩa.","nextBand":{"band":6.0,"descriptor":"Giữ độ chính xác ổn định ở cả câu phức."}}],
 "corrections":[
 {"excerpt":"to carry the extra weight","suggestion":"to bear the extra weight","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Lỗi ngữ pháp khác","fixHint":"Bear là cách diễn đạt trang trọng hơn carry trong văn viết học thuật.","severity":1,"reason":"Gợi ý nhẹ về cách diễn đạt.","certainty":"SUGGESTION","impedesMeaning":false}],
 "summary":"Bản viết lại tốt hơn rõ rệt: các lỗi hòa hợp chủ ngữ - động từ và số nhiều đã được sửa, bài đủ độ dài và có giải pháp cụ thể. Đạt band 6.0, bằng band đạt của đề.",
 "studyPoints":[{"kind":"CRITERION","criterionCode":"GRA","band":5.5,"code":"GRA","name":"Ngữ pháp","title":"Ngữ pháp","reason":"Giữ độ chính xác ở câu phức."}],
 "judgmentVersion":"kpj3","model":"demo"}$result$),
('task1-first', '22000000-0000-4000-8000-070000000001'::uuid, '22000000-0000-4000-8000-020000000006'::uuid, 5.5, interval '4 days',
$essay$The bar chart shows the average afternoon surface temperature of three kind of roof in three cities in July.

Overall, the black roofs are the hottest and the green roofs are the coolest in all the cities. Madrid is the hottest city and Copenhagen is the coolest for every roof type.

In Madrid, the black roof reaches 82 degrees, while the white roof is 45 degrees and the green roof is only 33 degrees. In Chicago, the temperatures is lower: 74 degrees for the black roof, 41 for the white roof and 30 for the green roof. Copenhagen has the lowest figures, with 61 degrees for black roofs, 35 for white roofs and 25 for green roofs.

The gap between the black and green roofs are biggest in Madrid, at 49 degrees, and smallest in Copenhagen, at 36 degrees.$essay$,
$prompt$
{"questionVersionId":"22000000-0000-4000-8000-070000000001","stem":"The bar chart below shows the average afternoon surface temperature of three types of roof in three cities in July. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.","task":"TASK_1","minWords":150,"passBand":6.0,"chartFacts":null,"sampleAnswer":null,"images":[],"knowledgePointIds":["22000000-0000-4000-8000-020000000006"]}$prompt$,
$result$
{"criteria":[
 {"code":"TA","band":5.5,"strengths":["Có câu tổng quan nêu xu hướng chính.","Số liệu nêu chính xác."],"improvements":["Có thể so sánh thêm giữa mái trắng và mái đen."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Mô tả đúng các đặc điểm chính nhưng ít so sánh.","nextBand":{"band":6.0,"descriptor":"Chọn lọc và so sánh các đặc điểm nổi bật."}},
 {"code":"CC","band":5.5,"strengths":["Chia đoạn theo tổng quan và chi tiết."],"improvements":["Dùng thêm từ nối so sánh như whereas, in contrast."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Bố cục rõ, liên kết còn đơn giản.","nextBand":{"band":6.0,"descriptor":"Liên kết đa dạng hơn."}},
 {"code":"LR","band":5.5,"strengths":["Dùng từ phù hợp để mô tả số liệu."],"improvements":["Lặp lại động từ is/are nhiều lần."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Vốn từ vừa đủ cho mô tả số liệu.","nextBand":{"band":6.0,"descriptor":"Đa dạng động từ mô tả."}},
 {"code":"GRA","band":5.0,"strengths":["Câu đúng cấu trúc ở phần lớn bài."],"improvements":["Lỗi hòa hợp chủ ngữ - động từ và số nhiều."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Có lỗi ngữ pháp lặp lại.","nextBand":{"band":5.5,"descriptor":"Giảm lỗi cơ bản."}}],
 "corrections":[
 {"excerpt":"three kind of roof","suggestion":"three kinds of roof","category":"GRAMMAR","errorCode":"PLURAL_FORM","errorName":"Sai dạng số nhiều","fixHint":"Sau three dùng danh từ số nhiều: kinds.","severity":2,"reason":"Thiếu -s sau three.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"the temperatures is lower","suggestion":"the temperatures are lower","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Hòa hợp chủ ngữ và động từ","fixHint":"Temperatures là số nhiều: are.","severity":2,"reason":"Chủ ngữ số nhiều nhưng dùng động từ số ít.","certainty":"ERROR","impedesMeaning":false},
 {"excerpt":"The gap between the black and green roofs are biggest","suggestion":"The gap between the black and green roofs is biggest","category":"GRAMMAR","errorCode":"GRAMMAR_OTHER","errorName":"Hòa hợp chủ ngữ và động từ","fixHint":"Chủ ngữ chính là gap (số ít): is.","severity":2,"reason":"Động từ phải hòa hợp với gap.","certainty":"ERROR","impedesMeaning":false}],
 "summary":"Bài mô tả đúng số liệu và có câu tổng quan. Cần giảm lỗi hòa hợp chủ ngữ - động từ và thêm so sánh giữa các loại mái để lên band 6.",
 "studyPoints":[{"kind":"CRITERION","criterionCode":"GRA","band":5.0,"code":"GRA","name":"Ngữ pháp","title":"Ngữ pháp","reason":"Hòa hợp chủ ngữ và động từ."}],
 "judgmentVersion":"kpj3","model":"demo"}$result$)
) AS e(code, question_version_id, kp, band, age, text, prompt, result)
WHERE NOT EXISTS (SELECT 1 FROM lesson_writing_submissions w WHERE w.id = md5('${demoUserId}:demo:essay:' || e.code)::uuid)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Lesson 11 practice that explains the blocking review: a failed attempt whose Writing item was graded by AI at band 5.5,
-- then an examiner review the learner asked for (Premium human grading credit). Free practice by knowledge point.
-- The examiner's side (review, job, comments) is written by the assessment callback with the same ids.
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, started_at, submitted_at,
                               request_id, correct_count, total_count, passed, counted_as_evidence, response, skills,
                               passed_skills, origin, knowledge_point_id)
SELECT md5('${demoUserId}:demo:practice:11:0')::uuid, u.id, l.lesson_id, l.skill, l.pkg, l.pkg_ver,
       now() - interval '6 hours', now() - interval '5 hours 30 minutes', md5('${demoUserId}:demo:practice-request:11:0')::uuid,
       2, 5, false, true, '{}'::jsonb, ARRAY[l.skill]::varchar[], '{}'::varchar[], 'LESSON', NULL
FROM demo_user u JOIN demo_lessons l ON l.n = 11
ON CONFLICT DO NOTHING;

INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id, knowledge_point_ids,
                                        request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                                        debit_ledger_entry_id, result, overall_band, passed, grading_started_at,
                                        submitted_at, graded_at, source, practice_attempt_id, prompt_version,
                                        previous_submission_id, revision_no, essay_hash)
SELECT md5('${demoUserId}:demo:essay:practice-item')::uuid, u.id, l.lesson_id, NULL,
       '21000000-0000-4000-8000-070000000001'::uuid, ARRAY['21000000-0000-4000-8000-020000000007']::uuid[],
       md5('${demoUserId}:demo:essay-request:practice-item')::uuid, e.text,
       cardinality(regexp_split_to_array(trim(e.text), '\s+')), e.prompt::jsonb, 'GRADED', 0,
       md5('${demoUserId}:demo:ledger:2')::uuid, e.result::jsonb, 5.5, false,
       now() - interval '5 hours 40 minutes', now() - interval '5 hours 40 minutes', now() - interval '5 hours 39 minutes',
       'PRACTICE_ITEM', md5('${demoUserId}:demo:practice:11:0')::uuid, 'essay-prompt-v3+tplv3', NULL, 1,
       md5(e.text) || md5(reverse(e.text))
FROM demo_user u
JOIN demo_lessons l ON l.n = 11
CROSS JOIN (VALUES (
$essay$I partly agree that new buildings should have green roofs. Green roofs help the city in many ways, but making them compulsory for every building is too strict.

The main benefit is that a green roof keeps a building cool, so people use less electricity for air conditioning. It also takes in rainwater, which reduces flooding in heavy storms. For example, many offices in Singapore have rooftop gardens and the streets nearby stay cooler.

On the other hand, green roofs cost a lot of money and need regular care. Small homeowners may not be able to pay for them, and some old roofs cannot carry the weight of soil and plants.

In conclusion, I think only large buildings should be required to have green roofs, and the government should offer help to smaller owners.$essay$,
$prompt$
{"questionVersionId":"21000000-0000-4000-8000-070000000001","stem":"Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.","task":"TASK_2","minWords":250,"passBand":6,"chartFacts":null,"sampleAnswer":null,"images":[],"knowledgePointIds":["21000000-0000-4000-8000-020000000007"]}$prompt$,
$result$
{"criteria":[
 {"code":"TR","band":5.5,"strengths":["Quan điểm rõ và nhất quán.","Có ví dụ Singapore."],"improvements":["Bài còn ngắn, mỗi lý do cần phát triển thêm."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Trả lời đúng đề, ý chưa được phát triển đủ.","nextBand":{"band":6.0,"descriptor":"Phát triển ý đầy đủ và đủ độ dài."}},
 {"code":"CC","band":6.0,"strengths":["Bố cục rõ ràng, từ nối đúng chỗ."],"improvements":[],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Bố cục và liên kết tốt.","nextBand":{"band":6.5,"descriptor":"Liên kết tự nhiên hơn."}},
 {"code":"LR","band":5.5,"strengths":["Từ vựng đủ dùng, ít lỗi."],"improvements":["Lặp lại cụm green roofs."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Vốn từ vừa đủ.","nextBand":{"band":6.0,"descriptor":"Đa dạng cách diễn đạt."}},
 {"code":"GRA","band":5.5,"strengths":["Ít lỗi ngữ pháp cơ bản."],"improvements":["Chưa dùng nhiều cấu trúc phức."],"knowledgePointJudgments":[],"evidence":[],"descriptor":"Ngữ pháp chính xác nhưng đơn giản.","nextBand":{"band":6.0,"descriptor":"Dùng thêm câu phức."}}],
 "corrections":[],
 "summary":"Bài ít lỗi nhưng còn ngắn, chưa đủ 250 từ nên chưa đạt band 6. Phát triển thêm mỗi lý do bằng giải thích và ví dụ.",
 "studyPoints":[{"kind":"AI","title":"Phát triển ý","reason":"Giải thích sâu hơn mỗi lý do để đạt đủ độ dài."}],
 "judgmentVersion":"kpj3","model":"demo"}$result$)
) AS e(text, prompt, result)
ON CONFLICT DO NOTHING;

-- The learner asked a teacher to grade that essay; the teacher finished it (the review lives in Assessment under the same id).
INSERT INTO human_review_requests (submission_id, user_id, review_submission_id, requested_at)
SELECT md5('${demoUserId}:demo:essay:practice-item')::uuid, u.id, md5('${demoUserId}:demo:human-review')::uuid,
       now() - interval '30 hours'
FROM demo_user u
ON CONFLICT DO NOTHING;

INSERT INTO human_review_outcomes (review_submission_id, user_id, submission_id, study_points, applied_at, overall_band, passed)
SELECT md5('${demoUserId}:demo:human-review')::uuid, u.id, md5('${demoUserId}:demo:essay:practice-item')::uuid,
       '[{"kind":"AI","title":"Phát triển ý","reason":"Mỗi lý do cần một ví dụ cụ thể và phần giải thích."}]'::jsonb,
       now() - interval '1 day', 6.0, true
FROM demo_user u
ON CONFLICT DO NOTHING;

-- Free practice by knowledge point: two passed and one failed, on the weakest knowledge points.
INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, started_at, submitted_at,
                               request_id, correct_count, total_count, passed, counted_as_evidence, response, skills,
                               passed_skills, origin, knowledge_point_id)
SELECT md5('${demoUserId}:demo:free-practice:' || f.n)::uuid, u.id, NULL, l.skill, l.pkg, l.pkg_ver,
       now() - f.age, now() - f.age + interval '8 minutes', md5('${demoUserId}:demo:free-practice-request:' || f.n)::uuid,
       f.correct, 5, f.correct >= 4, true, '{}'::jsonb, ARRAY[l.skill]::varchar[],
       CASE WHEN f.correct >= 4 THEN ARRAY[l.skill]::varchar[] ELSE '{}'::varchar[] END, 'FREE',
       (SELECT d.kp FROM demo_kps d WHERE d.first_lesson = f.lesson_n ORDER BY d.kp LIMIT 1)
FROM demo_user u
CROSS JOIN (VALUES (1, 3, interval '3 days', 4), (2, 5, interval '2 days', 5), (3, 6, interval '1 day', 2)) AS f(n, lesson_n, age, correct)
JOIN demo_lessons l ON l.n = f.lesson_n
ON CONFLICT DO NOTHING;
