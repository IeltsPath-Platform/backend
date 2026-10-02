-- A topic can require an Access feature to be learned, like packages and questions do (NULL = free). The demo seed
-- adds two PREMIUM_CONTENT topics after the free ones, so a free learner sees them but never has to pass them first.
ALTER TABLE topics ADD COLUMN required_feature_key VARCHAR(100);

INSERT INTO topics (id, code, name, sort_order, status, required_feature_key) VALUES
    ('24000000-0000-4000-8000-010000000001', 'PREMIUM_MATCHING_INFO', 'Matching Information', 930, 'ACTIVE', 'PREMIUM_CONTENT'),
    ('24000000-0000-4000-8000-010000000002', 'PREMIUM_SENTENCE_COMPLETION', 'Sentence Completion', 940, 'ACTIVE', 'PREMIUM_CONTENT');

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('24000000-0000-4000-8000-020000000001', '24000000-0000-4000-8000-010000000001', 'PR_MATCHING_INFORMATION', 'Tìm đoạn chứa thông tin', 'STRATEGY', 'PROCEDURE', 'READING', 'Tìm đoạn chứa một chi tiết cụ thể khi chi tiết được diễn đạt bằng từ khác.', 'ACTIVE'),
    ('24000000-0000-4000-8000-020000000002', '24000000-0000-4000-8000-010000000002', 'PR_SENTENCE_COMPLETION', 'Hoàn thành câu', 'STRATEGY', 'PROCEDURE', 'READING', 'Điền đúng từ trong bài, đúng dạng từ và đúng giới hạn số từ.', 'ACTIVE');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('24000000-0000-4000-8000-050000000001', 'PASSAGE', 'A. The first public libraries in Britain opened in the 1850s, funded by local taxes rather than private donors. B. By 1900 most large towns had one, although opening hours were often limited to the evenings. C. Today many libraries lend tools, musical instruments and even seeds, alongside books. D. A recent survey found that teenagers visit libraries mainly for quiet study space rather than to borrow books.', 'VALID'),
    ('24000000-0000-4000-8000-050000000002', 'PASSAGE', 'Honeybees communicate the location of food through a movement known as the waggle dance. The angle of the dance shows the direction of the food relative to the sun, while its duration indicates the distance. Researchers have found that bees which have never seen a dance can still learn the route, but they take roughly twice as long to find the food.', 'VALID');

-- Lessons: PM1, PM2 (Matching Information), PS1, PS2 (Sentence Completion)
INSERT INTO questions (id, question_type, skill, status) VALUES
    ('24000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000004', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('24000000-0000-4000-8000-070000000001', '24000000-0000-4000-8000-060000000001', 1, 'Passage: "A. Rents in the city centre doubled. B. Many families moved to the suburbs, where houses were cheaper." Which paragraph mentions the cost of homes outside the city?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"Houses were cheaper" ở đoạn B là giá nhà ngoài thành phố. Đoạn A nói giá thuê trong trung tâm.', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000002', '24000000-0000-4000-8000-060000000002', 1, 'Passage: "A. The museum was founded by a wealthy merchant. B. Entry has been free since 2001." Which paragraph mentions when visitors stopped paying?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"Stopped paying" được diễn đạt lại thành "entry has been free since 2001". "Wealthy" ở đoạn A là bẫy về tiền.', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000003', '24000000-0000-4000-8000-060000000003', 1, 'Passage: "The bridge was closed for repairs after heavy floods in March." Complete the sentence with ONE WORD from the passage: The bridge closed because of ______.', NULL, '{"type": "FILL", "accepted": ["floods"]}'::jsonb, 1, 'Từ cần điền nằm ngay sau "heavy". Câu cần danh từ số nhiều: floods.', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000004', '24000000-0000-4000-8000-060000000004', 1, 'Passage: "Volunteers planted over 3,000 trees along the river last spring." Complete the sentence with ONE WORD from the passage: The trees were planted by ______.', NULL, '{"type": "FILL", "accepted": ["volunteers"]}'::jsonb, 1, '"Planted by" hỏi người làm. Chủ ngữ của câu gốc là volunteers.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000001' WHERE id = '24000000-0000-4000-8000-060000000001';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000002' WHERE id = '24000000-0000-4000-8000-060000000002';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000003' WHERE id = '24000000-0000-4000-8000-060000000003';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000004' WHERE id = '24000000-0000-4000-8000-060000000004';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('24000000-0000-4000-8000-070000000001', '24000000-0000-4000-8000-020000000001', 1.00),
    ('24000000-0000-4000-8000-070000000002', '24000000-0000-4000-8000-020000000001', 1.00),
    ('24000000-0000-4000-8000-070000000003', '24000000-0000-4000-8000-020000000002', 1.00),
    ('24000000-0000-4000-8000-070000000004', '24000000-0000-4000-8000-020000000002', 1.00);

INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES
    ('24000000-0000-4000-8000-030000000001', '24000000-0000-4000-8000-010000000001', 'PM1', 'Quét đoạn tìm chi tiết', 1, 'PUBLISHED'),
    ('24000000-0000-4000-8000-030000000002', '24000000-0000-4000-8000-010000000001', 'PM2', 'Bẫy từ đồng nghĩa', 2, 'PUBLISHED'),
    ('24000000-0000-4000-8000-030000000003', '24000000-0000-4000-8000-010000000002', 'PS1', 'Tìm từ cần điền', 1, 'PUBLISHED'),
    ('24000000-0000-4000-8000-030000000004', '24000000-0000-4000-8000-010000000002', 'PS2', 'Giới hạn số từ và dạng từ', 2, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES
    ('24000000-0000-4000-8000-030000000001', '24000000-0000-4000-8000-020000000001'),
    ('24000000-0000-4000-8000-030000000002', '24000000-0000-4000-8000-020000000001'),
    ('24000000-0000-4000-8000-030000000003', '24000000-0000-4000-8000-020000000002'),
    ('24000000-0000-4000-8000-030000000004', '24000000-0000-4000-8000-020000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES
    ('24000000-0000-4000-8000-040000000001', '24000000-0000-4000-8000-030000000001', 1, 'TEXT', 'Dạng Matching Information hỏi đoạn nào chứa một chi tiết, không hỏi ý chính. Gạch chân từ khóa trong câu hỏi, rồi quét từng đoạn tìm cách diễn đạt khác của từ khóa đó. Một đoạn có thể chứa nhiều đáp án, hoặc không chứa đáp án nào.'),
    ('24000000-0000-4000-8000-040000000003', '24000000-0000-4000-8000-030000000002', 1, 'TEXT', 'Câu hỏi hầu như không dùng lại từ của bài. Đoạn có từ giống hệt câu hỏi thường là bẫy. Hãy so nghĩa: "stopped paying" có thể là "free", "cost" có thể là "cheaper".'),
    ('24000000-0000-4000-8000-040000000005', '24000000-0000-4000-8000-030000000003', 1, 'TEXT', 'Đọc câu cần hoàn thành trước, đoán loại từ còn thiếu (danh từ, động từ, số). Tìm đoạn chứa ý đó, rồi chép đúng từ trong bài, không đổi dạng.'),
    ('24000000-0000-4000-8000-040000000007', '24000000-0000-4000-8000-030000000004', 1, 'TEXT', 'Đề ghi "ONE WORD ONLY" thì điền hai từ là sai dù đúng nghĩa. Kiểm tra số ít, số nhiều và ngữ pháp của câu sau khi điền.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES
    ('24000000-0000-4000-8000-040000000002', '24000000-0000-4000-8000-030000000001', 2, 'EXERCISE'),
    ('24000000-0000-4000-8000-040000000004', '24000000-0000-4000-8000-030000000002', 2, 'EXERCISE'),
    ('24000000-0000-4000-8000-040000000006', '24000000-0000-4000-8000-030000000003', 2, 'EXERCISE'),
    ('24000000-0000-4000-8000-040000000008', '24000000-0000-4000-8000-030000000004', 2, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('24000000-0000-4000-8000-040000000002', '24000000-0000-4000-8000-070000000001', 1),
    ('24000000-0000-4000-8000-040000000004', '24000000-0000-4000-8000-070000000002', 1),
    ('24000000-0000-4000-8000-040000000006', '24000000-0000-4000-8000-070000000003', 1),
    ('24000000-0000-4000-8000-040000000008', '24000000-0000-4000-8000-070000000004', 1);

-- Final tests: X6 (Matching Information), X7 (Sentence Completion)
INSERT INTO content_packages (id, code, title, package_type, topic_id, status, required_feature_key) VALUES
    ('24000000-0000-4000-8000-080000000001', 'X6', 'Đề cuối X6: Public libraries', 'TOPIC_TEST', '24000000-0000-4000-8000-010000000001', 'PUBLISHED', 'PREMIUM_CONTENT'),
    ('24000000-0000-4000-8000-080000000002', 'X7', 'Đề cuối X7: The waggle dance', 'TOPIC_TEST', '24000000-0000-4000-8000-010000000002', 'PUBLISHED', 'PREMIUM_CONTENT');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('24000000-0000-4000-8000-090000000001', '24000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP),
    ('24000000-0000-4000-8000-090000000002', '24000000-0000-4000-8000-080000000002', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '24000000-0000-4000-8000-090000000001' WHERE id = '24000000-0000-4000-8000-080000000001';
UPDATE content_packages SET current_published_version_id = '24000000-0000-4000-8000-090000000002' WHERE id = '24000000-0000-4000-8000-080000000002';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES
    ('24000000-0000-4000-8000-0a0000000001', '24000000-0000-4000-8000-090000000001', 'Public libraries', 'READING', 1),
    ('24000000-0000-4000-8000-0a0000000002', '24000000-0000-4000-8000-090000000002', 'The waggle dance', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('24000000-0000-4000-8000-0c0000000001', '24000000-0000-4000-8000-050000000001', '24000000-0000-4000-8000-0a0000000001', 0),
    ('24000000-0000-4000-8000-0c0000000002', '24000000-0000-4000-8000-050000000002', '24000000-0000-4000-8000-0a0000000002', 0);

INSERT INTO questions (id, question_type, skill, status) VALUES
    ('24000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000007', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000008', 'FILL_IN_BLANK', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000009', 'FILL_IN_BLANK', 'READING', 'PUBLISHED'),
    ('24000000-0000-4000-8000-060000000010', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('24000000-0000-4000-8000-070000000005', '24000000-0000-4000-8000-060000000005', 1, 'Which paragraph mentions how the first libraries were paid for?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Đoạn A: "funded by local taxes rather than private donors".', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000006', '24000000-0000-4000-8000-060000000006', 1, 'Which paragraph mentions items other than books that can be borrowed?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Đoạn C: libraries lend tools, musical instruments and even seeds.', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000007', '24000000-0000-4000-8000-060000000007', 1, 'Which paragraph mentions why young people use libraries?', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}]'::jsonb, '{"type": "CHOICE", "correct": "D"}'::jsonb, 1, 'Đoạn D: teenagers visit libraries mainly for quiet study space.', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000008', '24000000-0000-4000-8000-060000000008', 1, 'Complete with ONE WORD from the passage: Bees share where food is through the waggle ______.', NULL, '{"type": "FILL", "accepted": ["dance"]}'::jsonb, 1, 'Câu đầu: "a movement known as the waggle dance".', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000009', '24000000-0000-4000-8000-060000000009', 1, 'Complete with ONE WORD from the passage: The direction of the food is shown relative to the ______.', NULL, '{"type": "FILL", "accepted": ["sun"]}'::jsonb, 1, '"The angle of the dance shows the direction of the food relative to the sun".', 'PUBLISHED'),
    ('24000000-0000-4000-8000-070000000010', '24000000-0000-4000-8000-060000000010', 1, 'Complete with ONE WORD from the passage: The length of the dance indicates the ______ to the food.', NULL, '{"type": "FILL", "accepted": ["distance"]}'::jsonb, 1, '"Its duration indicates the distance": length được diễn đạt lại thành duration.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000005' WHERE id = '24000000-0000-4000-8000-060000000005';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000006' WHERE id = '24000000-0000-4000-8000-060000000006';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000007' WHERE id = '24000000-0000-4000-8000-060000000007';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000008' WHERE id = '24000000-0000-4000-8000-060000000008';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000009' WHERE id = '24000000-0000-4000-8000-060000000009';
UPDATE questions SET current_published_version_id = '24000000-0000-4000-8000-070000000010' WHERE id = '24000000-0000-4000-8000-060000000010';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('24000000-0000-4000-8000-070000000005', '24000000-0000-4000-8000-020000000001', 1.00),
    ('24000000-0000-4000-8000-070000000006', '24000000-0000-4000-8000-020000000001', 1.00),
    ('24000000-0000-4000-8000-070000000007', '24000000-0000-4000-8000-020000000001', 1.00),
    ('24000000-0000-4000-8000-070000000008', '24000000-0000-4000-8000-020000000002', 1.00),
    ('24000000-0000-4000-8000-070000000009', '24000000-0000-4000-8000-020000000002', 1.00),
    ('24000000-0000-4000-8000-070000000010', '24000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('24000000-0000-4000-8000-0b0000000001', '24000000-0000-4000-8000-0a0000000001', '24000000-0000-4000-8000-070000000005', 1, 1.00),
    ('24000000-0000-4000-8000-0b0000000002', '24000000-0000-4000-8000-0a0000000001', '24000000-0000-4000-8000-070000000006', 2, 1.00),
    ('24000000-0000-4000-8000-0b0000000003', '24000000-0000-4000-8000-0a0000000001', '24000000-0000-4000-8000-070000000007', 3, 1.00),
    ('24000000-0000-4000-8000-0b0000000004', '24000000-0000-4000-8000-0a0000000002', '24000000-0000-4000-8000-070000000008', 1, 1.00),
    ('24000000-0000-4000-8000-0b0000000005', '24000000-0000-4000-8000-0a0000000002', '24000000-0000-4000-8000-070000000009', 2, 1.00),
    ('24000000-0000-4000-8000-0b0000000006', '24000000-0000-4000-8000-0a0000000002', '24000000-0000-4000-8000-070000000010', 3, 1.00);
