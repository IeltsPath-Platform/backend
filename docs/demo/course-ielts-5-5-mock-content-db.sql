-- Demo data only: fills the IELTS 5.5 course so every skill has many chặng of many lessons, the size the real course will
-- have (about 30-40 lessons per skill). It adds 16 chặng (4 per skill) of 6 lessons each next to the ones already there.
-- Every lesson is PUBLISHED with one short TEXT block linked to one knowledge point of its skill, so the lesson page opens
-- and each skill progresses on its own; there are no exercises, practice sets or chặng tests, so a chặng passes once all its
-- lessons are completed.
--
--   psql -h localhost -U postgres -d content_db -f backend/docs/demo/course-ielts-5-5-mock-content-db.sql
--   (or, with the Compose database: docker cp the file into ieltspath-postgres-1 and run psql -d content_db -f on it)
--
-- Safe to run again: a chặng or lesson whose code exists is skipped. Codes start with M55 so the rows are easy to find
-- and remove. It is a script and not a Flyway migration on purpose: a migration runs in every environment.
BEGIN;

CREATE TEMP TABLE mock_chang (
    skill VARCHAR(20), sort_order INT, code VARCHAR(100), name VARCHAR(255), description TEXT, lessons TEXT[]
) ON COMMIT DROP;

INSERT INTO mock_chang (skill, sort_order, code, name, description, lessons) VALUES
('LISTENING', 1011, 'M55_LISTENING_PART1', 'Part 1: Hội thoại đời thường',
 'Điền biểu mẫu, ghi tên, số và lịch hẹn trong hội thoại hằng ngày.',
 ARRAY['Điền biểu mẫu và số liệu', 'Ghi tên, địa chỉ, số điện thoại', 'Nghe ngày giờ và lịch hẹn', 'Bẫy sửa thông tin giữa chừng', 'Dạng điền ghi chú', 'Luyện tập tổng hợp Part 1']),
('LISTENING', 1012, 'M55_LISTENING_PART2', 'Part 2: Độc thoại về cơ sở vật chất',
 'Nghe mô tả địa điểm, chỉ đường và chọn đáp án theo ý người nói.',
 ARRAY['Nghe mô tả bản đồ', 'Chỉ đường và vị trí', 'Trắc nghiệm một đáp án', 'Dạng nối thông tin', 'Từ báo hiệu chuyển ý', 'Luyện tập tổng hợp Part 2']),
('LISTENING', 1013, 'M55_LISTENING_PART3', 'Part 3: Thảo luận học thuật',
 'Theo dõi hai đến bốn người thảo luận bài tập, nhận ra thái độ và quan điểm.',
 ARRAY['Nhận ra thái độ người nói', 'Nghe hai người bất đồng', 'Dạng nối đáp án', 'Chọn nhiều đáp án', 'Paraphrase khi nghe', 'Luyện tập tổng hợp Part 3']),
('LISTENING', 1014, 'M55_LISTENING_PART4', 'Part 4: Bài giảng học thuật',
 'Ghi chú một bài giảng dài, điền sơ đồ, bảng và số liệu.',
 ARRAY['Ghi chú bài giảng', 'Điền sơ đồ và bảng', 'Nghe số liệu và tỉ lệ', 'Theo dõi cấu trúc bài giảng', 'Từ vựng chủ đề khoa học', 'Luyện tập tổng hợp Part 4']),

('READING', 1021, 'M55_READING_SKIM_SCAN', 'Skimming và scanning',
 'Đọc lướt nắm ý chính và quét thông tin cụ thể trong thời gian giới hạn.',
 ARRAY['Đọc lướt nắm ý chính', 'Quét thông tin cụ thể', 'Xác định từ khóa', 'Nhận diện cấu trúc đoạn văn', 'Quản lý thời gian 60 phút', 'Luyện tập tổng hợp']),
('READING', 1022, 'M55_READING_MATCHING', 'Các dạng nối và phân loại',
 'Matching Headings, Features, Sentence Endings và Classification.',
 ARRAY['Matching Headings cơ bản', 'Bẫy từ khóa trùng trong Matching Headings', 'Matching Features', 'Matching Sentence Endings', 'Classification', 'Luyện tập tổng hợp']),
('READING', 1023, 'M55_READING_COMPLETION', 'Dạng điền và trả lời ngắn',
 'Summary, Note, Table Completion, Diagram Labelling và Short Answer.',
 ARRAY['Summary Completion', 'Note và Table Completion', 'Diagram Labelling', 'Short Answer Questions', 'Giới hạn số từ', 'Luyện tập tổng hợp']),
('READING', 1024, 'M55_READING_MCQ_YNNG', 'Multiple Choice và Yes/No/Not Given',
 'Chọn đáp án đúng và phân biệt quan điểm của tác giả.',
 ARRAY['Multiple Choice một đáp án', 'Chọn nhiều đáp án', 'Yes/No/Not Given: quan điểm tác giả', 'Phân biệt No và Not Given', 'Đoạn văn học thuật dài', 'Luyện tập tổng hợp']),

('WRITING', 1031, 'M55_WRITING_T1_CHARTS', 'Task 1: Biểu đồ đường và cột',
 'Cấu trúc bài Task 1, câu overview và mô tả xu hướng.',
 ARRAY['Cấu trúc bài Task 1', 'Viết câu overview', 'Mô tả xu hướng tăng giảm', 'So sánh số liệu', 'Dùng ngôn ngữ ước lượng', 'Luyện tập Task 1 biểu đồ']),
('WRITING', 1032, 'M55_WRITING_T1_OTHER', 'Task 1: Bảng, quy trình và bản đồ',
 'Mô tả bảng số liệu, quy trình và bản đồ thay đổi.',
 ARRAY['Mô tả bảng số liệu', 'Mô tả quy trình', 'Dùng bị động trong process', 'Mô tả bản đồ thay đổi', 'Từ chỉ vị trí và thay đổi', 'Luyện tập Task 1 tổng hợp']),
('WRITING', 1033, 'M55_WRITING_T2_BASICS', 'Task 2: Lập luận cơ bản',
 'Phân tích đề, mở bài, thân bài và kết bài.',
 ARRAY['Phân tích đề Task 2', 'Viết mở bài', 'Phát triển đoạn thân bài', 'Ví dụ và dẫn chứng', 'Viết kết bài', 'Luyện tập Task 2 cơ bản']),
('WRITING', 1034, 'M55_WRITING_T2_TYPES', 'Task 2: Các dạng đề',
 'Opinion, Discussion, Problem and Solution và các dạng khác.',
 ARRAY['Opinion essay', 'Discussion essay', 'Problem and Solution', 'Two-part question', 'Advantages and Disadvantages', 'Luyện tập Task 2 các dạng']),

('SPEAKING', 1041, 'M55_SPEAKING_PART1', 'Part 1: Giới thiệu bản thân',
 'Trả lời câu hỏi quen thuộc về bản thân, nhà cửa, công việc và sở thích.',
 ARRAY['Câu hỏi về gia đình', 'Câu hỏi về công việc và học tập', 'Câu hỏi về sở thích', 'Mở rộng câu trả lời', 'Tránh trả lời quá ngắn', 'Luyện tập tổng hợp Part 1']),
('SPEAKING', 1042, 'M55_SPEAKING_PART2', 'Part 2: Nói một chủ đề',
 'Nói liền mạch một đến hai phút theo cue card.',
 ARRAY['Đọc đề cue card', 'Lập dàn ý trong 1 phút', 'Kể về một người', 'Kể về một nơi chốn', 'Kể về một trải nghiệm', 'Luyện tập tổng hợp Part 2']),
('SPEAKING', 1043, 'M55_SPEAKING_PART3', 'Part 3: Thảo luận',
 'Trả lời câu hỏi trừu tượng, nêu quan điểm và lý do.',
 ARRAY['Trả lời câu hỏi trừu tượng', 'Đưa ra quan điểm', 'So sánh quá khứ và hiện tại', 'Dự đoán tương lai', 'Nêu lý do thuyết phục', 'Luyện tập tổng hợp Part 3']),
('SPEAKING', 1044, 'M55_SPEAKING_PRONUNCIATION', 'Phát âm và độ trôi chảy',
 'Trọng âm, ngữ điệu, nối âm và cách giữ nhịp khi nói.',
 ARRAY['Trọng âm từ', 'Ngữ điệu', 'Nối âm', 'Kỹ thuật kéo dài thời gian nói', 'Sửa lỗi phát âm hay gặp', 'Luyện tập tổng hợp']);

INSERT INTO topics (code, name, sort_order, status, skill, course_id, description)
SELECT m.code, m.name, m.sort_order, 'ACTIVE', m.skill, (SELECT id FROM courses WHERE code = 'IELTS_5_5'), m.description
  FROM mock_chang m
 WHERE NOT EXISTS (SELECT 1 FROM topics t WHERE t.code = m.code);

INSERT INTO lessons (topic_id, code, title, summary, summary_markdown, sort_order, status, estimated_minutes)
SELECT t.id, m.code || '-' || n.ord, n.title,
       'Bài học mẫu: ' || n.title,
       '## ' || n.title || E'\n\nNội dung mẫu để xem bố cục lộ trình. Bài này thuộc chặng "' || m.name || '".',
       n.ord, 'PUBLISHED', 8 + (n.ord * 7 + t.sort_order) % 8
  FROM mock_chang m
  JOIN topics t ON t.code = m.code
 CROSS JOIN LATERAL unnest(m.lessons) WITH ORDINALITY AS n(title, ord)
 WHERE NOT EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id = t.id AND l.code = m.code || '-' || n.ord);

INSERT INTO lesson_blocks (lesson_id, sort_order, block_type, text_content)
SELECT l.id, 1, 'TEXT', l.summary_markdown
  FROM lessons l
  JOIN topics t ON t.id = l.topic_id
 WHERE t.code LIKE 'M55\_%'
   AND NOT EXISTS (SELECT 1 FROM lesson_blocks b WHERE b.lesson_id = l.id);

-- The skill of a lesson, and so of its chặng, is read from the knowledge points of its blocks. One point per chặng, of the
-- chặng's skill, keeps each skill's chặng in its own sequence instead of one shared queue.
INSERT INTO knowledge_points (topic_id, code, name, kind, skill, description, status, learning_type)
SELECT t.id, m.code || '_KP', m.name,
       CASE WHEN m.code LIKE '%PRONUNCIATION' THEN 'PRONUNCIATION' ELSE 'STRATEGY' END,
       m.skill, m.description, 'ACTIVE', 'CONCEPT'
  FROM mock_chang m
  JOIN topics t ON t.code = m.code
 WHERE NOT EXISTS (SELECT 1 FROM knowledge_points k WHERE k.code = m.code || '_KP');

INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id)
SELECT l.id, k.id
  FROM lessons l
  JOIN topics t ON t.id = l.topic_id
  JOIN knowledge_points k ON k.code = t.code || '_KP'
 WHERE t.code LIKE 'M55\_%'
   AND NOT EXISTS (SELECT 1 FROM lesson_knowledge_points x WHERE x.lesson_id = l.id AND x.knowledge_point_id = k.id);

INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id)
SELECT b.id, k.id
  FROM lesson_blocks b
  JOIN lessons l ON l.id = b.lesson_id
  JOIN topics t ON t.id = l.topic_id
  JOIN knowledge_points k ON k.code = t.code || '_KP'
 WHERE t.code LIKE 'M55\_%' AND b.block_type = 'TEXT'
   AND NOT EXISTS (SELECT 1 FROM lesson_block_knowledge_points x WHERE x.block_id = b.id AND x.knowledge_point_id = k.id);

COMMIT;

SELECT t.skill, count(DISTINCT t.id) AS chang, count(l.id) FILTER (WHERE l.status = 'PUBLISHED') AS lessons
  FROM topics t
  JOIN courses c ON c.id = t.course_id AND c.code = 'IELTS_5_5'
  LEFT JOIN lessons l ON l.topic_id = t.id
 WHERE t.status = 'ACTIVE'
 GROUP BY t.skill
 ORDER BY t.skill;
