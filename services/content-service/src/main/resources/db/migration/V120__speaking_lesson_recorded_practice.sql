-- A recorded answer for each lesson of the Speaking chặng: one practice set per lesson with a single question the learner
-- answers by speaking (answer spec type SPEAKING). Learning grades the recording with AI inside the lesson's practice
-- attempt and applies the band to the lesson's knowledge point when that attempt is submitted. The multiple-choice sets
-- of V115 stay as they are, so a learner can clear the Practice without recording.
--
-- The four DEMO criterion knowledge points of the Speaking rubric (FC, LR, GRA, PR) are made INACTIVE: they have no lesson,
-- and a practice outside the course no longer writes mastery to them, so nothing would ever fill them. The rubric keeps
-- its criterion mapping; only the points leave the learner's path.
-- Idempotent: ids are fixed, and a lesson that does not exist (an older database) gets no set.

CREATE FUNCTION pg_temp.sid(kind text, n int) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT ('7e000000-0000-4000-8000-' || kind || lpad(n::text, 10, '0'))::uuid $$;

CREATE TEMP TABLE rec_set (k int, kp_code text, title text, prompt text, tip text) ON COMMIT DROP;
INSERT INTO rec_set VALUES
 (1, 'S55_PART1_FAMILIAR_ANSWERS', 'Nói Part 1: trả lời thẳng và thêm lý do',
  'Describe what you usually do at weekends. Answer the question directly first, then add a reason or an example.',
  'Start with a direct answer, then add one reason or example in the same breath.'),
 (2, 'S60_FLUENCY_COHERENCE', 'Nói trôi chảy và mạch lạc',
  'Talk about a time when you had to wait for something. Use simple linking words so that your ideas are easy to follow.',
  'Keep talking with short filler phrases and link your ideas with because, but, so and also.'),
 (3, 'S60_SPEAKING_VOCABULARY', 'Nói với từ vựng chính xác',
  'Describe a place you like to visit. Use precise words, and if you cannot remember a word, explain it in other words.',
  'Prefer precise words to "very + adjective", and describe a word you forget instead of stopping.'),
 (4, 'S65_GRAMMAR_RANGE', 'Nói với nhiều cấu trúc câu',
  'Talk about a change you would like to make in your daily life. Use some longer sentences with because, although or if.',
  'Mix short sentences with longer ones that use because, although, who or if, and keep them accurate.');

INSERT INTO questions (id, question_type, skill, status, purpose, created_at, updated_at)
SELECT pg_temp.sid('d4', r.k), 'SPEAKING', 'SPEAKING', 'PUBLISHED', 'LEARNING', now(), now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status, created_at, updated_at)
SELECT pg_temp.sid('d5', r.k), pg_temp.sid('d4', r.k), 1, r.prompt, NULL,
       '{"type":"SPEAKING","durationSeconds":60,"passBand":5.5}'::jsonb, 1, r.tip, 'PUBLISHED', now(), now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

UPDATE questions SET current_published_version_id = pg_temp.sid('d5', right(id::text, 10)::int)
 WHERE id::text LIKE '7e000000-0000-4000-8000-d4%' AND current_published_version_id IS NULL;

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT pg_temp.sid('d5', r.k), kp.id, 1.00
  FROM rec_set r JOIN knowledge_points kp ON kp.code = r.kp_code JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT DO NOTHING;

INSERT INTO content_packages (id, code, title, package_type, status, topic_id, lesson_id, created_at, updated_at)
SELECT pg_temp.sid('d1', r.k), 'SPK-REC-' || r.k, r.title, 'PRACTICE_SET', 'PUBLISHED', NULL, pg_temp.sid('03', r.k), now(), now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at, created_at, updated_at)
SELECT pg_temp.sid('d2', r.k), pg_temp.sid('d1', r.k), 1, 'PUBLISHED', '{}'::jsonb, 1, now(), now(), now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

UPDATE content_packages SET current_published_version_id = pg_temp.sid('d2', right(id::text, 10)::int)
 WHERE id::text LIKE '7e000000-0000-4000-8000-d1%' AND current_published_version_id IS NULL;

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, created_at, updated_at)
SELECT pg_temp.sid('d3', r.k), pg_temp.sid('d2', r.k), r.title, 'SPEAKING', 1, now(), now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, created_at)
SELECT pg_temp.sid('d6', r.k), pg_temp.sid('d3', r.k), pg_temp.sid('d5', r.k), 1, 1.00, now()
  FROM rec_set r JOIN lessons l ON l.id = pg_temp.sid('03', r.k)
ON CONFLICT (id) DO NOTHING;

UPDATE knowledge_points SET status = 'INACTIVE', updated_at = now()
 WHERE code IN ('DEMO_SPEAKING_CRITERION_FC', 'DEMO_SPEAKING_CRITERION_LR', 'DEMO_SPEAKING_CRITERION_GRA',
                'DEMO_SPEAKING_CRITERION_PR');

DROP FUNCTION pg_temp.sid(text, int);
