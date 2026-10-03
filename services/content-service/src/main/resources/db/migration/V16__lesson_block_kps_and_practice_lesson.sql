-- Learning needs to know which theory blocks teach a knowledge point, and which practice sets belong to a lesson.

-- A TEXT block teaches these knowledge points; a review of one of them shows only its blocks. Exercise blocks take
-- their knowledge points from their questions, and asset blocks (passages, audio) are never theory.
CREATE TABLE lesson_block_knowledge_points (
    block_id UUID NOT NULL REFERENCES lesson_blocks(id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    PRIMARY KEY (block_id, knowledge_point_id)
);
CREATE INDEX idx_lesson_block_kps_kp ON lesson_block_knowledge_points (knowledge_point_id);

-- Every seeded TEXT block teaches all knowledge points of its lesson (checked against the block texts), except the
-- one-line lead-in of L1 ("Luyện thêm với đoạn C và D."), which teaches nothing on its own.
INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id)
SELECT b.id, lkp.knowledge_point_id
FROM lesson_blocks b
JOIN lesson_knowledge_points lkp ON lkp.lesson_id = b.lesson_id
WHERE b.block_type = 'TEXT' AND b.id <> '20000000-0000-4000-8000-040000000004';

-- A practice set can belong to the lesson whose Practice it is; only practice sets may.
ALTER TABLE content_packages ADD COLUMN lesson_id UUID REFERENCES lessons(id);
ALTER TABLE content_packages ADD CONSTRAINT chk_package_lesson_practice
    CHECK (lesson_id IS NULL OR package_type = 'PRACTICE_SET');
CREATE INDEX idx_content_packages_lesson ON content_packages (lesson_id) WHERE lesson_id IS NOT NULL;

-- Existing practice sets of at least three questions join the earliest published lesson, of the same skill, that
-- teaches one of their knowledge points.
UPDATE content_packages target
SET lesson_id = pick.lesson_id
FROM (SELECT DISTINCT ON (p.id) p.id AS package_id, l.id AS lesson_id
      FROM content_packages p
      JOIN content_sections s ON s.package_version_id = p.current_published_version_id
      JOIN section_questions sq ON sq.section_id = s.id
      JOIN question_versions qv ON qv.id = sq.question_version_id
      JOIN questions q ON q.id = qv.question_id
      JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
      JOIN lesson_knowledge_points lkp ON lkp.knowledge_point_id = qkp.knowledge_point_id
      JOIN lessons l ON l.id = lkp.lesson_id AND l.status = 'PUBLISHED'
      JOIN topics t ON t.id = l.topic_id AND t.skill = q.skill
      WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED' AND p.lesson_id IS NULL
        AND (SELECT count(*) FROM content_sections s2 JOIN section_questions sq2 ON sq2.section_id = s2.id
             WHERE s2.package_version_id = p.current_published_version_id) >= 3
      ORDER BY p.id, t.sort_order, l.sort_order, l.id) pick
WHERE target.id = pick.package_id;

-- New practice sets: two more for DEMO_READING_MAIN_IDEA, so its review ladder has enough unseen sets, and one for
-- each lesson that had none (TF1, PM1, PM2, PS1, PS2). Every question is new and has a hint.

-- PRACTICE_SET PS-KP1-C
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000001', 'PS-KP1-C', 'Luyện thêm PS-KP1-C: Night trains', 'PRACTICE_SET', NULL, '20000000-0000-4000-8000-030000000002', 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000001', '26000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000001' WHERE id = '26000000-0000-4000-8000-080000000001';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000001', '26000000-0000-4000-8000-090000000001', 'Night trains', 'READING', 1);
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('26000000-0000-4000-8000-050000000001', 'PASSAGE', 'A. Night trains are returning to Europe after years of decline. New routes now link cities such as Vienna, Paris and Amsterdam, and several more are planned.

B. Travellers give two main reasons for choosing them. Many want to avoid the pollution caused by short flights, and others like arriving in a city centre after a night''s sleep instead of spending hours at an airport.

C. However, the comeback is fragile. Tickets often cost more than flights, and operators complain that old carriages and high track fees make the services hard to run at a profit.', 'VALID');
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('26000000-0000-4000-8000-0c0000000001', '26000000-0000-4000-8000-050000000001', '26000000-0000-4000-8000-0a0000000001', 0);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000001', '26000000-0000-4000-8000-060000000001', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "Night trains are cheaper than flights", "sortOrder": 1}, {"optionKey": "B", "content": "The return of night trains and the problems they face", "sortOrder": 2}, {"optionKey": "C", "content": "How to book a sleeper carriage", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Đoạn A, B nói tàu đêm quay lại và vì sao khách chọn; đoạn C nói khó khăn. A sai vì vé thường đắt hơn máy bay; C không được nhắc.', 'Ý chính của cả bài phải đúng với cả ba đoạn, kể cả đoạn C mở đầu bằng However.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000001' WHERE id = '26000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000001', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000001', '26000000-0000-4000-8000-0a0000000001', '26000000-0000-4000-8000-070000000001', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000002', '26000000-0000-4000-8000-060000000002', 1, 'What is the main idea of paragraph B?', '[{"optionKey": "A", "content": "Why travellers choose night trains", "sortOrder": 1}, {"optionKey": "B", "content": "Which cities have new routes", "sortOrder": 2}, {"optionKey": "C", "content": "How long flights take", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Đoạn B nêu hai lý do khách chọn tàu đêm. Tên các thành phố nằm ở đoạn A.', 'Câu đầu đoạn B báo trước nội dung cả đoạn: "two main reasons".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000002' WHERE id = '26000000-0000-4000-8000-060000000002';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000002', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000002', '26000000-0000-4000-8000-0a0000000001', '26000000-0000-4000-8000-070000000002', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000003', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000003', '26000000-0000-4000-8000-060000000003', 1, 'What is the main idea of paragraph C?', '[{"optionKey": "A", "content": "Night trains are popular with families", "sortOrder": 1}, {"optionKey": "B", "content": "Old carriages are being replaced", "sortOrder": 2}, {"optionKey": "C", "content": "Night trains struggle to make money", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Đoạn C nói tàu đêm khó có lãi: vé đắt, toa cũ, phí đường ray cao. Bài không nói toa cũ đang được thay.', '"However, the comeback is fragile" là câu chủ đề của đoạn C.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000003' WHERE id = '26000000-0000-4000-8000-060000000003';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000003', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000003', '26000000-0000-4000-8000-0a0000000001', '26000000-0000-4000-8000-070000000003', 3, 1.00);

-- PRACTICE_SET PS-KP1-D
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000002', 'PS-KP1-D', 'Luyện thêm PS-KP1-D: Paper or screen', 'PRACTICE_SET', NULL, '20000000-0000-4000-8000-030000000002', 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000002', '26000000-0000-4000-8000-080000000002', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000002' WHERE id = '26000000-0000-4000-8000-080000000002';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000002', '26000000-0000-4000-8000-090000000002', 'Paper or screen', 'READING', 1);
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES ('26000000-0000-4000-8000-050000000002', 'PASSAGE', 'A. More people now read news and books on screens than on paper. Phones and tablets are cheap, light and always at hand.

B. Yet studies suggest that screens change how well we understand. In several experiments, students who read long texts on paper remembered more of the details and the order of events than those who read the same texts on a tablet.

C. Researchers believe the reason is how we read rather than the device itself. On screens, people tend to scan quickly and jump between links, while printed pages encourage slower, deeper reading.', 'VALID');
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('26000000-0000-4000-8000-0c0000000002', '26000000-0000-4000-8000-050000000002', '26000000-0000-4000-8000-0a0000000002', 0);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000004', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000004', '26000000-0000-4000-8000-060000000004', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "Screen reading is common but may lead to weaker understanding", "sortOrder": 1}, {"optionKey": "B", "content": "Tablets are cheaper than books", "sortOrder": 2}, {"optionKey": "C", "content": "How to read a printed page", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Đọc trên màn hình phổ biến (đoạn A), hiểu kém hơn (đoạn B) và lý do (đoạn C). Bài không so sánh giá máy tính bảng với sách (B), và không hướng dẫn cách đọc (C).', 'Ý chính phải bao được cả ba đoạn, không chỉ đoạn đầu.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000004' WHERE id = '26000000-0000-4000-8000-060000000004';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000004', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000004', '26000000-0000-4000-8000-0a0000000002', '26000000-0000-4000-8000-070000000004', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000005', '26000000-0000-4000-8000-060000000005', 1, 'What is the main idea of paragraph B?', '[{"optionKey": "A", "content": "Students prefer tablets", "sortOrder": 1}, {"optionKey": "B", "content": "In studies, readers on paper remembered more", "sortOrder": 2}, {"optionKey": "C", "content": "Experiments are expensive", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Đoạn B mở bằng "Yet studies suggest…" rồi kể kết quả: đọc trên giấy nhớ nhiều hơn.', 'Từ "Yet" ở đầu đoạn B báo hiệu ý chính đổi hướng so với đoạn A.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000005' WHERE id = '26000000-0000-4000-8000-060000000005';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000005', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000005', '26000000-0000-4000-8000-0a0000000002', '26000000-0000-4000-8000-070000000005', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000006', '26000000-0000-4000-8000-060000000006', 1, 'Which title best fits the whole passage?', '[{"optionKey": "A", "content": "The History of the Tablet", "sortOrder": 1}, {"optionKey": "B", "content": "Paper or Screen: Does It Change How We Read?", "sortOrder": 2}, {"optionKey": "C", "content": "Why Students Dislike Long Texts", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'B bao cả sự phổ biến, kết quả nghiên cứu và lý do. A và C không phải nội dung chính của bài.', 'So từng tiêu đề với ý của cả ba đoạn, không chỉ với một đoạn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000006' WHERE id = '26000000-0000-4000-8000-060000000006';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000006', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000006', '26000000-0000-4000-8000-0a0000000002', '26000000-0000-4000-8000-070000000006', 3, 1.00);

-- PRACTICE_SET PS-TF-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000003', 'PS-TF-A', 'Luyện thêm PS-TF-A: False hay Not Given', 'PRACTICE_SET', NULL, '20000000-0000-4000-8000-030000000005', 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000003', '26000000-0000-4000-8000-080000000003', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000003' WHERE id = '26000000-0000-4000-8000-080000000003';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000003', '26000000-0000-4000-8000-090000000003', 'Luyện thêm PS-TF-A: False hay Not Given', 'READING', 1);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000007', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000007', '26000000-0000-4000-8000-060000000007', 1, 'Passage: "The swimming pool is closed every Monday for cleaning." Statement: "The pool opens on Mondays."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Đoạn văn nói hồ bơi đóng cửa mọi thứ Hai, mâu thuẫn trực tiếp với câu khẳng định: FALSE.', 'So câu khẳng định với ngày đóng cửa được nêu trong đoạn văn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000007' WHERE id = '26000000-0000-4000-8000-060000000007';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000007', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000007', '26000000-0000-4000-8000-0a0000000003', '26000000-0000-4000-8000-070000000007', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000008', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000008', '26000000-0000-4000-8000-060000000008', 1, 'Passage: "The bakery sells bread made with flour from local farms." Statement: "The bakery''s bread is cheaper than supermarket bread."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'Đoạn văn không nói gì về giá, nên không thể kết luận đúng hay sai: NOT GIVEN, không phải FALSE.', 'Với từng chi tiết của câu khẳng định, hỏi: đoạn văn xác nhận, phủ nhận, hay không nói tới?', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000008' WHERE id = '26000000-0000-4000-8000-060000000008';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000008', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000008', '26000000-0000-4000-8000-0a0000000003', '26000000-0000-4000-8000-070000000008', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000009', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000009', '26000000-0000-4000-8000-060000000009', 1, 'Passage: "All visitors must sign in at the front desk before entering the laboratory." Statement: "Visitors have to register before they go into the laboratory."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "TRUE"}'::jsonb, 1, '"sign in at the front desk" cùng nghĩa với "register": TRUE.', 'So từng phần của câu khẳng định (ai, làm gì, khi nào) với đoạn văn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000009' WHERE id = '26000000-0000-4000-8000-060000000009';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000009', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000009', '26000000-0000-4000-8000-0a0000000003', '26000000-0000-4000-8000-070000000009', 3, 1.00);

-- PRACTICE_SET PS-PM1-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000004', 'PS-PM1-A', 'Luyện thêm PS-PM1-A: Tìm đoạn chứa thông tin', 'PRACTICE_SET', NULL, '24000000-0000-4000-8000-030000000001', 'PUBLISHED', 'PREMIUM_CONTENT');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000004', '26000000-0000-4000-8000-080000000004', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000004' WHERE id = '26000000-0000-4000-8000-080000000004';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000004', '26000000-0000-4000-8000-090000000004', 'Luyện thêm PS-PM1-A: Tìm đoạn chứa thông tin', 'READING', 1);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000010', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000010', '26000000-0000-4000-8000-060000000010', 1, 'Passage: "A. The town was founded beside a river in 1820. B. A railway arrived in 1870 and trade grew quickly. C. Today most residents work in tourism." Which paragraph mentions how people earn a living now?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, '"Today most residents work in tourism" nói cách người dân kiếm sống hiện nay.', 'Tìm đoạn nói về công việc ở thời điểm hiện tại.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000010' WHERE id = '26000000-0000-4000-8000-060000000010';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000010', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000010', '26000000-0000-4000-8000-0a0000000004', '26000000-0000-4000-8000-070000000010', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000011', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000011', '26000000-0000-4000-8000-060000000011', 1, 'Passage: "A. The festival began as a small music event. B. Tickets sold out within an hour last year. C. Organisers plan to add a second stage." Which paragraph mentions how popular the event has become?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Vé bán hết trong một giờ cho thấy sự kiện rất được ưa chuộng.', 'Chi tiết nào cho thấy rất nhiều người muốn tham dự?', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000011' WHERE id = '26000000-0000-4000-8000-060000000011';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000011', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000011', '26000000-0000-4000-8000-0a0000000004', '26000000-0000-4000-8000-070000000011', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000012', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000012', '26000000-0000-4000-8000-060000000012', 1, 'Passage: "A. The bridge took four years to build. B. Engineers used steel shipped from abroad. C. Around 20,000 cars cross it every day." Which paragraph mentions where the building materials came from?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"steel shipped from abroad" cho biết vật liệu đến từ nước ngoài.', 'Vật liệu là gì, và đoạn nào nói nó đến từ đâu?', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000012' WHERE id = '26000000-0000-4000-8000-060000000012';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000012', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000012', '26000000-0000-4000-8000-0a0000000004', '26000000-0000-4000-8000-070000000012', 3, 1.00);

-- PRACTICE_SET PS-PM2-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000005', 'PS-PM2-A', 'Luyện thêm PS-PM2-A: Bẫy từ đồng nghĩa', 'PRACTICE_SET', NULL, '24000000-0000-4000-8000-030000000002', 'PUBLISHED', 'PREMIUM_CONTENT');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000005', '26000000-0000-4000-8000-080000000005', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000005' WHERE id = '26000000-0000-4000-8000-080000000005';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000005', '26000000-0000-4000-8000-090000000005', 'Luyện thêm PS-PM2-A: Bẫy từ đồng nghĩa', 'READING', 1);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000013', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000013', '26000000-0000-4000-8000-060000000013', 1, 'Passage: "A. The school has a large library. B. Students can borrow laptops free of charge. C. The library is closed on Sundays." Which paragraph mentions something that costs nothing?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"free of charge" nghĩa là không tốn tiền. Đoạn A và C lặp từ "library" nhưng không nói về chi phí.', 'Câu hỏi không dùng từ "free"; tìm cách nói khác của "costs nothing".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000013' WHERE id = '26000000-0000-4000-8000-060000000013';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000013', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000013', '26000000-0000-4000-8000-0a0000000005', '26000000-0000-4000-8000-070000000013', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000014', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000014', '26000000-0000-4000-8000-060000000014', 1, 'Passage: "A. The company was set up by two brothers. B. Its first shop opened in a small village. C. It now employs over 500 people." Which paragraph mentions the size of the workforce?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, '"employs over 500 people" cho biết quy mô lực lượng lao động.', '"workforce" là những người làm việc cho công ty.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000014' WHERE id = '26000000-0000-4000-8000-060000000014';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000014', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000014', '26000000-0000-4000-8000-0a0000000005', '26000000-0000-4000-8000-070000000014', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000015', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000015', '26000000-0000-4000-8000-060000000015', 1, 'Passage: "A. Rain fell for six days without stopping. B. Rivers rose and several roads were flooded. C. Farmers say the wet weather damaged their crops." Which paragraph mentions harm to food production?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, '"damaged their crops" là thiệt hại cho sản xuất lương thực; đoạn B nói đường bị ngập, không nói về lương thực.', 'Diễn đạt "food production" bằng từ khác, rồi tìm cách nói đó trong từng đoạn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000015' WHERE id = '26000000-0000-4000-8000-060000000015';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000015', '24000000-0000-4000-8000-020000000001', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000015', '26000000-0000-4000-8000-0a0000000005', '26000000-0000-4000-8000-070000000015', 3, 1.00);

-- PRACTICE_SET PS-PS1-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000006', 'PS-PS1-A', 'Luyện thêm PS-PS1-A: Tìm từ cần điền', 'PRACTICE_SET', NULL, '24000000-0000-4000-8000-030000000003', 'PUBLISHED', 'PREMIUM_CONTENT');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000006', '26000000-0000-4000-8000-080000000006', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000006' WHERE id = '26000000-0000-4000-8000-080000000006';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000006', '26000000-0000-4000-8000-090000000006', 'Luyện thêm PS-PS1-A: Tìm từ cần điền', 'READING', 1);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000016', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000016', '26000000-0000-4000-8000-060000000016', 1, 'Passage: "The castle was rebuilt in stone after a fire destroyed its walls, which were made of timber." Complete the sentence with ONE WORD from the passage: The first walls were built from ______.', NULL, '{"type": "FILL", "accepted": ["timber"]}'::jsonb, 1, '"walls, which were made of timber": từ cần điền là "timber".', 'Câu cần một danh từ chỉ vật liệu; tìm vật liệu của bức tường cũ.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000016' WHERE id = '26000000-0000-4000-8000-060000000016';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000016', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000016', '26000000-0000-4000-8000-0a0000000006', '26000000-0000-4000-8000-070000000016', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000017', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000017', '26000000-0000-4000-8000-060000000017', 1, 'Passage: "Most of the island''s electricity now comes from wind turbines." Complete the sentence with ONE WORD from the passage: Wind provides most of the island''s ______.', NULL, '{"type": "FILL", "accepted": ["electricity"]}'::jsonb, 1, 'Câu gốc: "Most of the island''s electricity … comes from wind": từ cần điền là "electricity".', 'Tìm thứ mà gió cung cấp cho hòn đảo.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000017' WHERE id = '26000000-0000-4000-8000-060000000017';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000017', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000017', '26000000-0000-4000-8000-0a0000000006', '26000000-0000-4000-8000-070000000017', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000018', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000018', '26000000-0000-4000-8000-060000000018', 1, 'Passage: "The museum asks visitors to leave large bags in the lockers near the entrance." Complete the sentence with ONE WORD from the passage: Large bags must be left in the ______ near the entrance.', NULL, '{"type": "FILL", "accepted": ["lockers"]}'::jsonb, 1, 'Chép đúng dạng số nhiều trong bài: "lockers".', 'Cần một danh từ chỉ nơi để túi xách.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000018' WHERE id = '26000000-0000-4000-8000-060000000018';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000018', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000018', '26000000-0000-4000-8000-0a0000000006', '26000000-0000-4000-8000-070000000018', 3, 1.00);

-- PRACTICE_SET PS-PS2-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key) VALUES ('26000000-0000-4000-8000-080000000007', 'PS-PS2-A', 'Luyện thêm PS-PS2-A: Giới hạn số từ và dạng từ', 'PRACTICE_SET', NULL, '24000000-0000-4000-8000-030000000004', 'PUBLISHED', 'PREMIUM_CONTENT');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('26000000-0000-4000-8000-090000000007', '26000000-0000-4000-8000-080000000007', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '26000000-0000-4000-8000-090000000007' WHERE id = '26000000-0000-4000-8000-080000000007';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('26000000-0000-4000-8000-0a0000000007', '26000000-0000-4000-8000-090000000007', 'Luyện thêm PS-PS2-A: Giới hạn số từ và dạng từ', 'READING', 1);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000019', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000019', '26000000-0000-4000-8000-060000000019', 1, 'Passage: "Researchers counted the birds every morning for three months." Complete the sentence with ONE WORD from the passage: The birds were counted daily for three ______.', NULL, '{"type": "FILL", "accepted": ["months"]}'::jsonb, 1, 'Sau "three" cần danh từ số nhiều: "months". Viết "month" là sai.', 'Sau một con số lớn hơn một, danh từ phải ở dạng nào?', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000019' WHERE id = '26000000-0000-4000-8000-060000000019';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000019', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000019', '26000000-0000-4000-8000-0a0000000007', '26000000-0000-4000-8000-070000000019', 1, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000020', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000020', '26000000-0000-4000-8000-060000000020', 1, 'Passage: "The new library will open to the public in early September." Complete the sentence with NO MORE THAN TWO WORDS from the passage: The library will open in ______.', NULL, '{"type": "FILL", "accepted": ["early September", "September"]}'::jsonb, 1, '"early September" (hai từ) hoặc "September" đều nằm trong giới hạn hai từ.', 'Đề cho tối đa hai từ; chọn cụm chỉ thời gian trong bài.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000020' WHERE id = '26000000-0000-4000-8000-060000000020';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000020', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000020', '26000000-0000-4000-8000-0a0000000007', '26000000-0000-4000-8000-070000000020', 2, 1.00);
INSERT INTO questions (id, question_type, skill, status) VALUES ('26000000-0000-4000-8000-060000000021', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('26000000-0000-4000-8000-070000000021', '26000000-0000-4000-8000-060000000021', 1, 'Passage: "Visitors are advised to book tickets online to avoid long queues." Complete the sentence with ONE WORD from the passage: Booking online helps visitors avoid long ______.', NULL, '{"type": "FILL", "accepted": ["queues"]}'::jsonb, 1, 'Cần danh từ số nhiều "queues", giữ nguyên dạng trong bài.', 'Từ cần điền đứng sau tính từ "long".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '26000000-0000-4000-8000-070000000021' WHERE id = '26000000-0000-4000-8000-060000000021';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('26000000-0000-4000-8000-070000000021', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('26000000-0000-4000-8000-0b0000000021', '26000000-0000-4000-8000-0a0000000007', '26000000-0000-4000-8000-070000000021', 3, 1.00);

-- Every published lesson of a topic with a final test must offer Practice: learners need it to unlock the test.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM lessons l
               WHERE l.status = 'PUBLISHED'
                 AND EXISTS (SELECT 1 FROM content_packages tp
                             WHERE tp.topic_id = l.topic_id AND tp.package_type = 'TOPIC_TEST' AND tp.status = 'PUBLISHED')
                 AND NOT EXISTS (SELECT 1 FROM content_packages p
                                 WHERE p.lesson_id = l.id AND p.package_type = 'PRACTICE_SET'
                                   AND p.status = 'PUBLISHED' AND p.current_published_version_id IS NOT NULL)) THEN
        RAISE EXCEPTION 'Every published lesson of a topic with a final test needs a published practice set';
    END IF;
END $$;
