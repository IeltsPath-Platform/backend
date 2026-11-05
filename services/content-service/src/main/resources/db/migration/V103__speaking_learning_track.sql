-- Speaking learning track: SPEAKING_FRAMEWORK joins IELTS 6.5 with four knowledge points that have a lesson and a
-- practice set, plus a Speaking error catalog (SPK_*). The other four Speaking knowledge points stay INACTIVE until they
-- have content. Idempotent: ids are fixed, so a database that already holds this content is left as it is.
--
-- The exercises are objective (multiple choice), so the mastery they give is knowledge of speaking strategy, not a
-- measure of speaking. No error code below has a trap knowledge point, so a wrong answer never writes evidence into
-- another skill's knowledge point.

CREATE FUNCTION pg_temp.sid(kind text, n int) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT ('7e000000-0000-4000-8000-' || kind || lpad(n::text, 10, '0'))::uuid $$;

-- ---------------------------------------------------------------------------------------------------------------------
-- Source rows
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_kp (k int, code text, lesson_code text, lesson_title text, lesson_text text, pkg_title text) ON COMMIT DROP;
INSERT INTO seed_kp VALUES
 (1, 'S55_PART1_FAMILIAR_ANSWERS', 'S55-1', 'Part 1: trả lời thẳng và thêm lý do',
  E'Trong Part 1, giám khảo hỏi những câu quen thuộc (quê quán, công việc, sở thích).\n\nCông thức ngắn: **trả lời thẳng trước, thêm một lý do hoặc ví dụ sau**. Một câu "Yes." hay "No." là quá ngắn; kể cả câu chuyện dài cũng không cần thiết. Hai đến ba câu là đủ.\n\nVí dụ: "Do you like cooking?" → "Yes, I do, mainly because it helps me relax after a long day at work."',
  'Luyện Part 1: trả lời thẳng và thêm lý do'),
 (2, 'S60_FLUENCY_COHERENCE', 'S60-1', 'Nói trôi chảy và mạch lạc',
  E'Trôi chảy không có nghĩa là nói nhanh. Người nói tốt vẫn ngập ngừng, nhưng họ dùng **cụm câu đệm** ("Well, let me think...", "That\'s an interesting question") thay vì im lặng hay "um um".\n\nDùng **từ nối đơn giản** để các ý liền nhau: because (lý do), but (đối lập), so (kết quả), also (thêm ý).\n\nKhi quên từ, đừng dừng lại xin lỗi: hãy diễn đạt lại ("What I mean is...").',
  'Luyện nói trôi chảy và mạch lạc'),
 (3, 'S60_SPEAKING_VOCABULARY', 'S60-2', 'Từ vựng theo chủ đề và cách diễn đạt khi thiếu từ',
  E'Bạn không cần từ khó, nhưng cần từ **chính xác** theo chủ đề: "enormous" thay cho "very big", "delicious" thay cho "very good".\n\nKhi quên một từ, hãy **mô tả nó** thay vì im lặng hoặc nói tiếng Việt: "the shop where you buy medicine" thay cho "pharmacy".\n\nChú ý các cụm từ đi cùng nhau: make a decision, take a break, do homework.',
  'Luyện từ vựng khi nói'),
 (4, 'S65_GRAMMAR_RANGE', 'S65-1', 'Đa dạng cấu trúc câu khi nói',
  E'Để đạt band cao, bạn cần trộn **câu đơn và câu phức**: mệnh đề với because, although, if; mệnh đề quan hệ với who, which; câu điều kiện.\n\nNhưng độ chính xác quan trọng không kém: for/since, hòa hợp chủ vị (she doesn\'t), thì của động từ.\n\nVí dụ: "I drink tea every day because it helps me relax." (một ý phụ với because).',
  'Luyện cấu trúc câu khi nói');

-- seq = k * 10 + n. n 1..2 sit in the lesson exercise block; n 3..5 only in the practice set, because a practice set
-- must not reuse a lesson question.
CREATE TEMP TABLE seed_q (k int, n int, stem text, a text, b text, c text, correct text, expl text) ON COMMIT DROP;
INSERT INTO seed_q VALUES
 (1,1,'The examiner asks: "Do you like cooking?" Which answer is best for Part 1?','Yes.','Yes, I do, mainly because it helps me relax after a long day at work.','Cooking is a very broad topic and there are many kinds of it.','B','B answers directly and adds a reason in a natural, short way.'),
 (1,2,'What should you do first when you answer a familiar Part 1 question?','Give a direct answer, then add a reason or example.','Tell a long story about your childhood.','Ask the examiner to choose another question.','A','Start with a direct answer, then support it briefly.'),
 (1,3,'The examiner asks: "Where are you from?" Which answer is best?','Vietnam.','I''m from Da Nang, a coastal city in central Vietnam, so it is famous for its beaches.','I don''t know how to explain it.','B','B gives the answer and an extra detail in one or two sentences.'),
 (1,4,'Which sentence adds a reason to "I enjoy reading."?','I read.','It helps me learn new words and forget about stress.','Reading is reading.','B','B gives a real reason; the others repeat the idea.'),
 (1,5,'About how long should a typical Part 1 answer be?','One word','Two or three sentences','Five minutes','B','Part 1 answers are short but not one word: two or three sentences is a good target.'),
 (2,1,'Which phrases help you keep talking naturally while you think?','"Well, let me think..." / "That''s an interesting question."','"Um um um um..." / long silence.','"I don''t know." / "Next question."','A','Filler phrases keep the answer flowing and sound natural.'),
 (2,2,'Which linking word shows a reason?','However','Because','Although','B','"Because" introduces a reason.'),
 (2,3,'Complete the sentence to show contrast: "I like the city, ___ it is very noisy."','so','but','because','B','"But" links two contrasting ideas.'),
 (2,4,'You lose a word in the middle of an answer. What is the best thing to do?','Stop and apologise several times.','Rephrase it, for example "What I mean is..."','Switch to Vietnamese.','B','Rephrasing keeps you speaking and shows flexibility.'),
 (2,5,'Which answer is the most coherent?','I like winter. Because I like warm clothes. Also holidays. But rain.','I prefer winter, mainly because I enjoy wearing warm clothes. Also, there are some great holidays, so the season feels special.','Winter warm clothes holidays I like rain.','B','B links the ideas with because, also and so, and each sentence is complete.'),
 (3,1,'You forget the word "pharmacy". What is the best way to continue?','Stay silent until you remember it.','Say "the shop where you buy medicine".','Say the word in Vietnamese.','B','Describing the word keeps you speaking and shows vocabulary range.'),
 (3,2,'Which word is more precise than "very big"?','enormous','big big','not small','A','"Enormous" is a precise, stronger adjective.'),
 (3,3,'Choose a more specific word for "good": "The food was very good."','delicious','okay','food','A','"Delicious" is more specific than "very good" for food.'),
 (3,4,'Which collocation is natural?','do a decision','make a decision','take a decision of','B','We say "make a decision".'),
 (3,5,'Which phrase best paraphrases "a lot of people"?','a large number of people','much people','many of peoples','A','"A large number of people" is correct and more formal.'),
 (4,1,'Which sentence joins two ideas with a subordinate clause?','I like tea. I drink it daily.','I drink tea every day because it helps me relax.','Tea. Every day.','B','"Because it helps me relax" is a subordinate clause.'),
 (4,2,'Which sentence is correct?','I have lived here since five years.','I have lived here for five years.','I live here since five years.','B','Use "for" with a period of time and the present perfect.'),
 (4,3,'Which sentence uses a second conditional correctly?','If I will have time, I travel.','If I had more time, I would travel more.','If I have had time, I would travelled.','B','Second conditional: if + past simple, would + base verb.'),
 (4,4,'Which sentence is correct?','She don''t like spicy food.','She doesn''t like spicy food.','She not like spicy food.','B','Third person singular takes "doesn''t".'),
 (4,5,'Which sentence contains a relative clause?','I met a man who speaks three languages.','I met a man. Three languages.','Man speak three languages I meet.','A','"who speaks three languages" is a relative clause.');

-- Wrong option -> error code. Only codes without a trap knowledge point.
CREATE TEMP TABLE seed_dis (seq int, opt text, code text) ON COMMIT DROP;
INSERT INTO seed_dis VALUES
 (11,'A','SPK_NO_REASON'),(11,'C','SPK_OFF_QUESTION'),
 (12,'B','SPK_OFF_QUESTION'),(12,'C','SPK_OFF_QUESTION'),
 (13,'A','SPK_NO_REASON'),(13,'C','SPK_OFF_QUESTION'),
 (14,'A','SPK_NO_REASON'),(14,'C','SPK_NO_REASON'),
 (15,'A','SPK_NO_REASON'),(15,'C','SPK_OFF_QUESTION'),
 (21,'B','SPK_FILLER_OVERUSE'),(21,'C','SPK_REPAIR_FAILURE'),
 (22,'A','SPK_WRONG_LINKER'),(22,'C','SPK_WRONG_LINKER'),
 (23,'A','SPK_WRONG_LINKER'),(23,'C','SPK_WRONG_LINKER'),
 (24,'A','SPK_REPAIR_FAILURE'),(24,'C','SPK_REPAIR_FAILURE'),
 (25,'A','SPK_NO_LINKER'),(25,'C','SPK_NO_LINKER'),
 (31,'A','SPK_REPAIR_FAILURE'),(31,'C','SPK_REPAIR_FAILURE'),
 (32,'B','SPK_IMPRECISE_WORD'),(32,'C','SPK_IMPRECISE_WORD'),
 (33,'B','SPK_IMPRECISE_WORD'),(33,'C','SPK_IMPRECISE_WORD'),
 (34,'A','SPK_WRONG_COLLOCATION'),(34,'C','SPK_WRONG_COLLOCATION'),
 (35,'B','SPK_WRONG_COLLOCATION'),(35,'C','SPK_WRONG_COLLOCATION'),
 (41,'A','SPK_SIMPLE_STRUCTURES_ONLY'),(41,'C','SENTENCE_FRAGMENT'),
 (42,'A','TENSE'),(42,'C','TENSE'),
 (43,'A','TENSE'),(43,'C','TENSE'),
 (44,'A','SUBJECT_VERB_AGREEMENT'),(44,'C','SUBJECT_VERB_AGREEMENT'),
 (45,'B','SPK_SIMPLE_STRUCTURES_ONLY'),(45,'C','SENTENCE_FRAGMENT');

-- ---------------------------------------------------------------------------------------------------------------------
-- Error catalog (error_types allows no SPEAKING category, so the existing ones are reused)
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO error_types (id, code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, trap_knowledge_point_id, created_at, updated_at)
SELECT gen_random_uuid(), t.code, t.name, t.category, t.severity, kp.id, t.fix_hint, 'ACTIVE', 'APPROVED', NULL, now(), now()
  FROM (VALUES
    ('SPK_NO_REASON',             'Trả lời quá ngắn, thiếu lý do',        'TASK',       2, 'S55_PART1_FAMILIAR_ANSWERS', 'Sau câu trả lời trực tiếp, thêm một lý do hoặc ví dụ bằng "because" hoặc "for example".'),
    ('SPK_OFF_QUESTION',          'Trả lời lệch câu hỏi',                 'TASK',       3, 'S55_PART1_FAMILIAR_ANSWERS', 'Nghe kỹ từ khóa của câu hỏi và trả lời đúng ý đó trước khi nói thêm.'),
    ('SPK_FILLER_OVERUSE',        'Ngập ngừng, lạm dụng âm đệm',          'COHERENCE',  1, 'S60_FLUENCY_COHERENCE',      'Thay "um, uh" bằng cụm đệm như "Well, let me think" hoặc "That is an interesting question".'),
    ('SPK_NO_LINKER',             'Thiếu từ nối giữa các ý',              'COHERENCE',  2, 'S60_FLUENCY_COHERENCE',      'Nối các ý bằng từ nối đơn giản: because, but, so, also.'),
    ('SPK_WRONG_LINKER',          'Dùng từ nối sai chức năng khi nói',    'COHERENCE',  1, 'S60_FLUENCY_COHERENCE',      'Chọn từ nối theo quan hệ giữa hai ý: nguyên nhân (because), đối lập (but), kết quả (so).'),
    ('SPK_REPAIR_FAILURE',        'Không diễn đạt lại khi quên từ',       'VOCABULARY', 2, 'S60_SPEAKING_VOCABULARY',    'Khi quên từ, mô tả nó bằng từ đơn giản hơn ("the shop where you buy medicine") thay vì dừng hoặc nói tiếng Việt.'),
    ('SPK_IMPRECISE_WORD',        'Dùng từ chung chung, thiếu chính xác', 'VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY',    'Thay "very good", "very big" bằng từ cụ thể hơn như "delicious", "enormous".'),
    ('SPK_WRONG_COLLOCATION',     'Kết hợp từ không tự nhiên khi nói',    'VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY',    'Học từ theo cụm: make a decision, take a break, do homework.'),
    ('SPK_SIMPLE_STRUCTURES_ONLY','Chỉ dùng câu đơn, thiếu câu phức',     'GRAMMAR',    2, 'S65_GRAMMAR_RANGE',          'Nối hai ý thành một câu bằng mệnh đề với because, although, who hoặc which.')
  ) AS t(code, name, category, severity, kp_code, fix_hint)
  JOIN knowledge_points kp ON kp.code = t.kp_code
 WHERE NOT EXISTS (SELECT 1 FROM error_types e WHERE e.code = t.code);

-- ---------------------------------------------------------------------------------------------------------------------
-- Topic into the IELTS 6.5 course after Writing (980); only the four KPs with content go ACTIVE.
-- An ACTIVE knowledge point needs learning_type: GRAMMAR = CONCEPT, VOCABULARY = MEMORY, STRATEGY = PROCEDURE.
-- ---------------------------------------------------------------------------------------------------------------------
UPDATE topics SET status = 'ACTIVE', course_id = '30000000-0000-4000-8000-000000000002', sort_order = 990, updated_at = now()
 WHERE code = 'SPEAKING_FRAMEWORK';
UPDATE knowledge_points SET status = 'ACTIVE', updated_at = now(),
       learning_type = CASE kind WHEN 'GRAMMAR' THEN 'CONCEPT' WHEN 'VOCABULARY' THEN 'MEMORY' ELSE 'PROCEDURE' END
 WHERE code IN (SELECT code FROM seed_kp);

-- ---------------------------------------------------------------------------------------------------------------------
-- Lessons: one TEXT block and one EXERCISE block each
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO lessons (id, topic_id, code, title, summary, sort_order, status, summary_markdown, estimated_minutes)
SELECT pg_temp.sid('03', s.k), t.id, s.lesson_code, s.lesson_title, '', s.k, 'PUBLISHED', s.lesson_text, 8
  FROM seed_kp s, topics t WHERE t.code = 'SPEAKING_FRAMEWORK'
ON CONFLICT (id) DO NOTHING;
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id)
SELECT pg_temp.sid('03', s.k), kp.id FROM seed_kp s JOIN knowledge_points kp ON kp.code = s.code
ON CONFLICT DO NOTHING;
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content, created_at, updated_at)
SELECT pg_temp.sid('04', s.k * 10 + 1), pg_temp.sid('03', s.k), 1, 'TEXT', s.lesson_text, now(), now() FROM seed_kp s
UNION ALL
SELECT pg_temp.sid('04', s.k * 10 + 2), pg_temp.sid('03', s.k), 2, 'EXERCISE', NULL, now(), now() FROM seed_kp s
ON CONFLICT (id) DO NOTHING;
INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id)
SELECT pg_temp.sid('04', s.k * 10 + 1), kp.id FROM seed_kp s JOIN knowledge_points kp ON kp.code = s.code
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- Questions (all LEARNING, MULTIPLE_CHOICE), each tied to its KP
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO questions (id, question_type, skill, status, purpose, created_at, updated_at)
SELECT pg_temp.sid('06', q.k * 10 + q.n), 'MULTIPLE_CHOICE', 'SPEAKING', 'PUBLISHED', 'LEARNING', now(), now() FROM seed_q q
ON CONFLICT (id) DO NOTHING;
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status, created_at, updated_at)
SELECT pg_temp.sid('07', q.k * 10 + q.n), pg_temp.sid('06', q.k * 10 + q.n), 1, q.stem,
       jsonb_build_array(
         jsonb_build_object('content', q.a, 'optionKey', 'A', 'sortOrder', 1),
         jsonb_build_object('content', q.b, 'optionKey', 'B', 'sortOrder', 2),
         jsonb_build_object('content', q.c, 'optionKey', 'C', 'sortOrder', 3)),
       jsonb_build_object('type', 'CHOICE', 'correct', q.correct)
         || COALESCE((SELECT jsonb_build_object('distractors', jsonb_object_agg(d.opt, d.code))
                        FROM seed_dis d WHERE d.seq = q.k * 10 + q.n), '{}'::jsonb),
       1, q.expl, 'PUBLISHED', now(), now()
  FROM seed_q q
ON CONFLICT (id) DO NOTHING;
UPDATE questions SET current_published_version_id = pg_temp.sid('07', right(id::text, 10)::int)
 WHERE id::text LIKE '7e000000-0000-4000-8000-06%' AND current_published_version_id IS NULL;
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT pg_temp.sid('07', q.k * 10 + q.n), kp.id, 1.00
  FROM seed_q q JOIN seed_kp s ON s.k = q.k JOIN knowledge_points kp ON kp.code = s.code
ON CONFLICT DO NOTHING;

-- Lesson exercise block: n = 1, 2.
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order)
SELECT pg_temp.sid('04', q.k * 10 + 2), pg_temp.sid('07', q.k * 10 + q.n), q.n FROM seed_q q WHERE q.n <= 2
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------------------------------------------------
-- One practice set per KP: n = 3, 4, 5
-- ---------------------------------------------------------------------------------------------------------------------
INSERT INTO content_packages (id, code, title, package_type, status, topic_id, lesson_id, created_at, updated_at)
SELECT pg_temp.sid('08', s.k), 'SPK-PS-' || s.k, s.pkg_title, 'PRACTICE_SET', 'PUBLISHED', NULL, pg_temp.sid('03', s.k), now(), now() FROM seed_kp s
ON CONFLICT (id) DO NOTHING;
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at, created_at, updated_at)
SELECT pg_temp.sid('09', s.k), pg_temp.sid('08', s.k), 1, 'PUBLISHED', '{}'::jsonb, 1, now(), now(), now() FROM seed_kp s
ON CONFLICT (id) DO NOTHING;
UPDATE content_packages SET current_published_version_id = pg_temp.sid('09', right(id::text, 10)::int)
 WHERE id::text LIKE '7e000000-0000-4000-8000-08%' AND current_published_version_id IS NULL;
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, created_at, updated_at)
SELECT pg_temp.sid('0a', s.k), pg_temp.sid('09', s.k), s.pkg_title, 'SPEAKING', 1, now(), now() FROM seed_kp s
ON CONFLICT (id) DO NOTHING;
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, created_at)
SELECT pg_temp.sid('0b', q.k * 10 + q.n), pg_temp.sid('0a', q.k), pg_temp.sid('07', q.k * 10 + q.n), q.n - 2, 1.00, now() FROM seed_q q WHERE q.n >= 3
ON CONFLICT (id) DO NOTHING;

DROP FUNCTION pg_temp.sid(text, int);
