-- Listening demo: topic DEMO_LISTENING with lessons LS1-LS2, one practice set per knowledge point and two final
-- test codes (X3, X4). Audio is stored as an object key resolved against CONTENT_MEDIA_BASE_URL; the transcript
-- (asset text) is an answer and only leaves Content through the internal API.

INSERT INTO topics (id, code, name, sort_order, status) VALUES ('23000000-0000-4000-8000-010000000001', 'DEMO_LISTENING', 'Demo IELTS Listening', 920, 'ACTIVE');

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('23000000-0000-4000-8000-020000000008', '23000000-0000-4000-8000-010000000001', 'LS_NUM', 'Nghe số, ngày, giờ, giá', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Nghe và ghi đúng số, ngày, giờ, giá tiền, kể cả khi người nói đổi ý.', 'ACTIVE'),
    ('23000000-0000-4000-8000-020000000009', '23000000-0000-4000-8000-010000000001', 'LS_SPELL', 'Nghe đánh vần tên, mã', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Ghi đúng tên riêng và mã được đánh vần từng chữ.', 'ACTIVE'),
    ('23000000-0000-4000-8000-020000000010', '23000000-0000-4000-8000-010000000001', 'LS_PARA', 'Bắt ý qua paraphrase', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Nhận ra đáp án được diễn đạt lại bằng từ khác với bài nghe.', 'ACTIVE'),
    ('23000000-0000-4000-8000-020000000011', '23000000-0000-4000-8000-010000000001', 'LS_TRAP', 'Tránh bẫy đổi ý', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Bỏ qua thông tin bị nhắc tới rồi bị sửa hoặc gạt đi.', 'ACTIVE');

-- Audio (8 files to upload under the media base URL)
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000001', 'AUDIO', 'Librarian: Good morning, Riverside Library. How can I help you?
Caller: Hi. I would like to book a study room for my group.
Librarian: Of course. Can I have your surname, please?
Caller: Yes, it is Thompson. T, H, O, M, P, S, O, N.
Librarian: Thank you. Which day would you like the room?
Caller: Friday the fifteenth of May, in the afternoon, please.
Librarian: Fine. A room for up to six people costs twelve pounds for three hours.
Caller: Twelve pounds is fine.
Librarian: And could I have a contact number?
Caller: Sure. It is 0 7 7 0 0, 9 0 0, 3 1 4.', 'listening/demo/ls1.mp3', 45, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000002', 'AUDIO', 'Officer: Good evening, everyone. Tonight I want to explain the new bike-sharing scheme, which starts next month.
Officer: The council is not trying to make money from it. The main aim is to cut the number of short car journeys in the city centre, because almost half of all car trips here are under three kilometres.
Officer: Bikes will be available at forty stations, and the first thirty minutes of every ride will be free.
Officer: Some residents are worried about safety, so we are also building protected cycle lanes along the river.
Officer: Finally, the scheme will be reviewed after one year. If it works well, it will be extended to the suburbs.', 'listening/demo/ls2.mp3', 50, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000003', 'AUDIO', 'Customer: Hi, two tickets for the seven o''clock film on Saturday, please.
Clerk: Sorry, seven o''clock is sold out. There is another showing at nine thirty.
Customer: OK, nine thirty then. How much is that?
Clerk: Two adults, that is eighteen pounds fifty altogether.
Clerk: You are in row F, seats twelve and thirteen.', 'listening/demo/numM.mp3', 30, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000004', 'AUDIO', 'Receptionist: City Fitness, how can I help?
Caller: I want to join. My surname is Okafor, O, K, A, F, O, R.
Receptionist: And the company you work for?
Caller: Brightwell. B, R, I, G, H, T, W, E, L, L. Two Ls at the end.
Receptionist: Great. And your email username?
Caller: It is J, A, Y... sorry, J, A, Y, E, then the number eighty-eight.', 'listening/demo/spellM.mp3', 35, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000005', 'AUDIO', 'Guide: Welcome back to the Harbour Museum, which reopened last week after two years of repairs.
Guide: The old building had serious problems with damp, and many objects could not be shown safely. That was the main reason for closing.
Guide: Now, for the first time, visitors can also see the collection of ship models, which used to be kept in storage.
Guide: Entry is still free, but we ask visitors to book a time online at weekends, when it gets very busy.
Guide: We also hope to open a small cafe on the roof next spring.', 'listening/demo/museum.mp3', 50, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000006', 'AUDIO', 'Anna: Shall we meet on Thursday to plan the trip?
Ben: Thursday is difficult for me. Could we make it Friday instead?
Anna: Friday is fine. Let us meet at the station.
Ben: Actually, the cafe next to the library is quieter. Let us go there.
Anna: Good idea. I will bring some sandwiches.
Ben: No need, the cafe does lunch. Just bring the map.', 'listening/demo/trapM.mp3', 30, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000007', 'AUDIO', 'Receptionist: Good evening, Harbour View Hotel.
Guest: Hello. I would like to book a room. My name is Delaney. D, E, L, A, N, E, Y.
Receptionist: Thank you, Ms Delaney. How many nights?
Guest: Four nights, from the twelfth of July.
Receptionist: Certainly. Can I ask how you heard about us?
Guest: A colleague stayed here last year. She said the rooms were quiet even though the hotel is near the station.
Receptionist: That is good to hear. Breakfast is included, but parking is charged separately.', 'listening/demo/hotel.mp3', 45, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('23000000-0000-4000-8000-050000000008', 'AUDIO', 'Guide: Hello and welcome. The walking tour used to start at ten, but it now leaves at ten thirty from the fountain in Market Square.
Guide: My name is Ms Fenn, that is F, E, N, N, and I will be your guide for about two hours.
Guide: We focus on the history of the old wool trade rather than on famous buildings, so you will visit several workshops that most tourists never see.
Guide: Please do not bring large bags. A small backpack is fine.', 'listening/demo/tour.mp3', 40, 'VALID');

-- Lesson questions
-- LQ1 (KP9, LS1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000001', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000001', '23000000-0000-4000-8000-060000000001', 1, 'Surname: ______', NULL, '{"type": "FILL", "accepted": ["thompson"]}'::jsonb, 1, 'Người gọi đánh vần T-H-O-M-P-S-O-N.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000001' WHERE id = '23000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000001', '23000000-0000-4000-8000-020000000009', 1.00);
-- LQ2 (KP8, LS1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000002', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000002', '23000000-0000-4000-8000-060000000002', 1, 'Date: Friday ______ May', NULL, '{"type": "FILL", "accepted": ["15", "15th", "fifteenth", "the 15th", "the fifteenth"]}'::jsonb, 1, '"Friday the fifteenth of May".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000002' WHERE id = '23000000-0000-4000-8000-060000000002';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000002', '23000000-0000-4000-8000-020000000008', 1.00);
-- LQ3 (KP8, LS1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000003', '23000000-0000-4000-8000-060000000003', 1, 'Cost for three hours: £______', NULL, '{"type": "FILL", "accepted": ["12", "twelve"]}'::jsonb, 1, '"costs twelve pounds for three hours".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000003' WHERE id = '23000000-0000-4000-8000-060000000003';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000003', '23000000-0000-4000-8000-020000000008', 1.00);
-- LQ4 (KP8, LS1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000004', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000004', '23000000-0000-4000-8000-060000000004', 1, 'Contact number: ______', NULL, '{"type": "FILL", "accepted": ["07700900314", "07700 900314", "07700 900 314"]}'::jsonb, 1, '"0 7 7 0 0, 9 0 0, 3 1 4".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000004' WHERE id = '23000000-0000-4000-8000-060000000004';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000004', '23000000-0000-4000-8000-020000000008', 1.00);
-- LQ5 (KP10, LS2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000005', '23000000-0000-4000-8000-060000000005', 1, 'What is the main purpose of the scheme?', '[{"optionKey": "A", "content": "To raise money for the council", "sortOrder": 1}, {"optionKey": "B", "content": "To reduce short car trips in the centre", "sortOrder": 2}, {"optionKey": "C", "content": "To attract more tourists", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"not trying to make money… cut the number of short car journeys" được diễn đạt lại thành B.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000005' WHERE id = '23000000-0000-4000-8000-060000000005';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000005', '23000000-0000-4000-8000-020000000010', 1.00);
-- LQ6 (KP11, LS2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000006', '23000000-0000-4000-8000-060000000006', 1, 'What will the council do because of safety concerns?', '[{"optionKey": "A", "content": "Limit each ride to thirty minutes", "sortOrder": 1}, {"optionKey": "B", "content": "Build protected lanes by the river", "sortOrder": 2}, {"optionKey": "C", "content": "Close some streets in the centre", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"protected cycle lanes along the river". Ba mươi phút là bẫy: đó là thời gian miễn phí, không liên quan an toàn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000006' WHERE id = '23000000-0000-4000-8000-060000000006';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000006', '23000000-0000-4000-8000-020000000011', 1.00);
-- LQ7 (KP10, LS2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000007', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000007', '23000000-0000-4000-8000-060000000007', 1, 'What may happen after the first year?', '[{"optionKey": "A", "content": "The scheme will end", "sortOrder": 1}, {"optionKey": "B", "content": "Riders will start paying", "sortOrder": 2}, {"optionKey": "C", "content": "The scheme will cover outer areas of the city", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, '"extended to the suburbs" = cover outer areas.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000007' WHERE id = '23000000-0000-4000-8000-060000000007';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000007', '23000000-0000-4000-8000-020000000010', 1.00);

-- Lessons and blocks
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('23000000-0000-4000-8000-030000000001', '23000000-0000-4000-8000-010000000001', 'LS1', 'Điền form khi nghe gọi điện', 1, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('23000000-0000-4000-8000-030000000001', '23000000-0000-4000-8000-020000000008');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('23000000-0000-4000-8000-030000000001', '23000000-0000-4000-8000-020000000009');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('23000000-0000-4000-8000-040000000001', '23000000-0000-4000-8000-030000000001', 1, 'TEXT', 'Dạng form completion: đọc trước các chỗ trống và đoán loại thông tin cần nghe (tên, ngày, số tiền, số điện thoại). Mẹo: Tên riêng thường được đánh vần từng chữ. Viết đúng chính tả: sai một chữ là sai cả câu.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('23000000-0000-4000-8000-040000000002', '23000000-0000-4000-8000-030000000001', 2, 'ASSET', '23000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('23000000-0000-4000-8000-040000000003', '23000000-0000-4000-8000-030000000001', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000003', '23000000-0000-4000-8000-070000000001', 1);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000003', '23000000-0000-4000-8000-070000000002', 2);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000003', '23000000-0000-4000-8000-070000000003', 3);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000003', '23000000-0000-4000-8000-070000000004', 4);
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('23000000-0000-4000-8000-030000000002', '23000000-0000-4000-8000-010000000001', 'LS2', 'Bắt ý chính khi nghe nói', 2, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('23000000-0000-4000-8000-030000000002', '23000000-0000-4000-8000-020000000010');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('23000000-0000-4000-8000-030000000002', '23000000-0000-4000-8000-020000000011');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('23000000-0000-4000-8000-040000000004', '23000000-0000-4000-8000-030000000002', 1, 'TEXT', 'Đáp án đúng hiếm khi lặp lại đúng từ trong bài nghe mà diễn đạt lại (paraphrase). Lựa chọn có từ giống hệt bài nghe thường là bẫy. Mẹo: Để ý câu phủ định kiểu "not to make money, but to…": ý chính nằm sau "but".');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('23000000-0000-4000-8000-040000000005', '23000000-0000-4000-8000-030000000002', 2, 'ASSET', '23000000-0000-4000-8000-050000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('23000000-0000-4000-8000-040000000006', '23000000-0000-4000-8000-030000000002', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000006', '23000000-0000-4000-8000-070000000005', 1);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000006', '23000000-0000-4000-8000-070000000006', 2);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('23000000-0000-4000-8000-040000000006', '23000000-0000-4000-8000-070000000007', 3);

-- TOPIC_TEST X3
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000001', 'X3', 'Đề cuối X3: Đặt phòng khách sạn', 'TOPIC_TEST', '23000000-0000-4000-8000-010000000001', 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000001', '23000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000001' WHERE id = '23000000-0000-4000-8000-080000000001';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000001', '23000000-0000-4000-8000-090000000001', 'Đặt phòng khách sạn', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000001', '23000000-0000-4000-8000-050000000007', '23000000-0000-4000-8000-0a0000000001', 0);
-- HT1 (KP9, X3)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000008', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000008', '23000000-0000-4000-8000-060000000008', 1, 'Surname: ______', NULL, '{"type": "FILL", "accepted": ["delaney"]}'::jsonb, 1, 'D-E-L-A-N-E-Y.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000008' WHERE id = '23000000-0000-4000-8000-060000000008';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000008', '23000000-0000-4000-8000-020000000009', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000001', '23000000-0000-4000-8000-0a0000000001', '23000000-0000-4000-8000-070000000008', 1, 1.00);
-- HT2 (KP8, X3)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000009', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000009', '23000000-0000-4000-8000-060000000009', 1, 'Number of nights: ______', NULL, '{"type": "FILL", "accepted": ["4", "four"]}'::jsonb, 1, '"Four nights".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000009' WHERE id = '23000000-0000-4000-8000-060000000009';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000009', '23000000-0000-4000-8000-020000000008', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000002', '23000000-0000-4000-8000-0a0000000001', '23000000-0000-4000-8000-070000000009', 2, 1.00);
-- HT3 (KP10, X3)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000010', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000010', '23000000-0000-4000-8000-060000000010', 1, 'Why did the guest choose this hotel?', '[{"optionKey": "A", "content": "It is the cheapest near the station", "sortOrder": 1}, {"optionKey": "B", "content": "Someone she works with recommended it", "sortOrder": 2}, {"optionKey": "C", "content": "She saw an online advert", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"A colleague stayed here" = someone she works with.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000010' WHERE id = '23000000-0000-4000-8000-060000000010';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000010', '23000000-0000-4000-8000-020000000010', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000003', '23000000-0000-4000-8000-0a0000000001', '23000000-0000-4000-8000-070000000010', 3, 1.00);
-- HT4 (KP11, X3)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000011', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000011', '23000000-0000-4000-8000-060000000011', 1, 'What must guests pay extra for?', '[{"optionKey": "A", "content": "Breakfast", "sortOrder": 1}, {"optionKey": "B", "content": "Parking", "sortOrder": 2}, {"optionKey": "C", "content": "Wi-Fi", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Bẫy: bữa sáng được nhắc trước nhưng đã gồm trong giá; "parking is charged separately".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000011' WHERE id = '23000000-0000-4000-8000-060000000011';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000011', '23000000-0000-4000-8000-020000000011', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000004', '23000000-0000-4000-8000-0a0000000001', '23000000-0000-4000-8000-070000000011', 4, 1.00);

-- TOPIC_TEST X4
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000002', 'X4', 'Đề cuối X4: Tour đi bộ quanh phố cổ', 'TOPIC_TEST', '23000000-0000-4000-8000-010000000001', 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000002', '23000000-0000-4000-8000-080000000002', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000002' WHERE id = '23000000-0000-4000-8000-080000000002';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000002', '23000000-0000-4000-8000-090000000002', 'Tour đi bộ quanh phố cổ', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000002', '23000000-0000-4000-8000-050000000008', '23000000-0000-4000-8000-0a0000000002', 0);
-- TR1 (KP8, X4)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000012', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000012', '23000000-0000-4000-8000-060000000012', 1, 'Start time: ______', NULL, '{"type": "FILL", "accepted": ["10.30", "10:30", "ten thirty", "half past ten"]}'::jsonb, 1, 'Bẫy: trước đây là mười giờ, nay "ten thirty".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000012' WHERE id = '23000000-0000-4000-8000-060000000012';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000012', '23000000-0000-4000-8000-020000000008', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000005', '23000000-0000-4000-8000-0a0000000002', '23000000-0000-4000-8000-070000000012', 1, 1.00);
-- TR2 (KP9, X4)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000013', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000013', '23000000-0000-4000-8000-060000000013', 1, 'Guide''s name: Ms ______', NULL, '{"type": "FILL", "accepted": ["fenn"]}'::jsonb, 1, 'F-E-N-N.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000013' WHERE id = '23000000-0000-4000-8000-060000000013';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000013', '23000000-0000-4000-8000-020000000009', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000006', '23000000-0000-4000-8000-0a0000000002', '23000000-0000-4000-8000-070000000013', 2, 1.00);
-- TR3 (KP10, X4)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000014', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000014', '23000000-0000-4000-8000-060000000014', 1, 'What is the tour mainly about?', '[{"optionKey": "A", "content": "Famous buildings in the old town", "sortOrder": 1}, {"optionKey": "B", "content": "The town''s history of making and selling wool", "sortOrder": 2}, {"optionKey": "C", "content": "Shopping in the market", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"history of the old wool trade rather than famous buildings".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000014' WHERE id = '23000000-0000-4000-8000-060000000014';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000014', '23000000-0000-4000-8000-020000000010', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000007', '23000000-0000-4000-8000-0a0000000002', '23000000-0000-4000-8000-070000000014', 3, 1.00);
-- TR4 (KP11, X4)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000015', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000015', '23000000-0000-4000-8000-060000000015', 1, 'What can people bring on the tour?', '[{"optionKey": "A", "content": "Large bags", "sortOrder": 1}, {"optionKey": "B", "content": "A small backpack", "sortOrder": 2}, {"optionKey": "C", "content": "Nothing at all", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"do not bring large bags. A small backpack is fine." Large bags là bẫy vì được nhắc tới đầu tiên.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000015' WHERE id = '23000000-0000-4000-8000-060000000015';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000015', '23000000-0000-4000-8000-020000000011', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000008', '23000000-0000-4000-8000-0a0000000002', '23000000-0000-4000-8000-070000000015', 4, 1.00);

-- PRACTICE_SET PS-NUM
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000003', 'PS-NUM', 'Luyện thêm PS-NUM: Mua vé xem phim', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000003', '23000000-0000-4000-8000-080000000003', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000003' WHERE id = '23000000-0000-4000-8000-080000000003';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000003', '23000000-0000-4000-8000-090000000003', 'Mua vé xem phim', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000003', '23000000-0000-4000-8000-050000000003', '23000000-0000-4000-8000-0a0000000003', 0);
-- NM1 (KP8, PS-NUM)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000016', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000016', '23000000-0000-4000-8000-060000000016', 1, 'Showing time: ______', NULL, '{"type": "FILL", "accepted": ["9.30", "9:30", "nine thirty", "half past nine"]}'::jsonb, 1, 'Bẫy: suất bảy giờ đã hết vé, nên là "nine thirty".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000016' WHERE id = '23000000-0000-4000-8000-060000000016';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000016', '23000000-0000-4000-8000-020000000008', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000009', '23000000-0000-4000-8000-0a0000000003', '23000000-0000-4000-8000-070000000016', 1, 1.00);
-- NM2 (KP8, PS-NUM)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000017', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000017', '23000000-0000-4000-8000-060000000017', 1, 'Total price: £______', NULL, '{"type": "FILL", "accepted": ["18.50", "18.5"]}'::jsonb, 1, '"eighteen pounds fifty altogether".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000017' WHERE id = '23000000-0000-4000-8000-060000000017';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000017', '23000000-0000-4000-8000-020000000008', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000010', '23000000-0000-4000-8000-0a0000000003', '23000000-0000-4000-8000-070000000017', 2, 1.00);
-- NM3 (KP8, PS-NUM)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000018', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000018', '23000000-0000-4000-8000-060000000018', 1, 'First seat number: ______', NULL, '{"type": "FILL", "accepted": ["12", "twelve"]}'::jsonb, 1, '"seats twelve and thirteen".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000018' WHERE id = '23000000-0000-4000-8000-060000000018';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000018', '23000000-0000-4000-8000-020000000008', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000011', '23000000-0000-4000-8000-0a0000000003', '23000000-0000-4000-8000-070000000018', 3, 1.00);

-- PRACTICE_SET PS-SPELL
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000004', 'PS-SPELL', 'Luyện thêm PS-SPELL: Đăng ký phòng tập', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000004', '23000000-0000-4000-8000-080000000004', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000004' WHERE id = '23000000-0000-4000-8000-080000000004';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000004', '23000000-0000-4000-8000-090000000004', 'Đăng ký phòng tập', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000004', '23000000-0000-4000-8000-050000000004', '23000000-0000-4000-8000-0a0000000004', 0);
-- SM1 (KP9, PS-SPELL)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000019', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000019', '23000000-0000-4000-8000-060000000019', 1, 'Surname: ______', NULL, '{"type": "FILL", "accepted": ["okafor"]}'::jsonb, 1, 'O-K-A-F-O-R.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000019' WHERE id = '23000000-0000-4000-8000-060000000019';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000019', '23000000-0000-4000-8000-020000000009', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000012', '23000000-0000-4000-8000-0a0000000004', '23000000-0000-4000-8000-070000000019', 1, 1.00);
-- SM2 (KP9, PS-SPELL)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000020', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000020', '23000000-0000-4000-8000-060000000020', 1, 'Company: ______', NULL, '{"type": "FILL", "accepted": ["brightwell"]}'::jsonb, 1, 'B-R-I-G-H-T-W-E-L-L, hai chữ L ở cuối.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000020' WHERE id = '23000000-0000-4000-8000-060000000020';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000020', '23000000-0000-4000-8000-020000000009', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000013', '23000000-0000-4000-8000-0a0000000004', '23000000-0000-4000-8000-070000000020', 2, 1.00);
-- SM3 (KP9, PS-SPELL)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000021', 'FILL_IN_BLANK', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000021', '23000000-0000-4000-8000-060000000021', 1, 'Email username: ______', NULL, '{"type": "FILL", "accepted": ["jaye88"]}'::jsonb, 1, 'Bẫy: người gọi sửa J-A-Y thành J-A-Y-E, rồi số 88.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000021' WHERE id = '23000000-0000-4000-8000-060000000021';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000021', '23000000-0000-4000-8000-020000000009', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000014', '23000000-0000-4000-8000-0a0000000004', '23000000-0000-4000-8000-070000000021', 3, 1.00);

-- PRACTICE_SET PS-PARA
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000005', 'PS-PARA', 'Luyện thêm PS-PARA: Bảo tàng mở cửa lại', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000005', '23000000-0000-4000-8000-080000000005', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000005' WHERE id = '23000000-0000-4000-8000-080000000005';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000005', '23000000-0000-4000-8000-090000000005', 'Bảo tàng mở cửa lại', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000005', '23000000-0000-4000-8000-050000000005', '23000000-0000-4000-8000-0a0000000005', 0);
-- MU1 (KP10, PS-PARA)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000022', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000022', '23000000-0000-4000-8000-060000000022', 1, 'Why did the museum close?', '[{"optionKey": "A", "content": "To build a new cafe", "sortOrder": 1}, {"optionKey": "B", "content": "Damp was harming the building and objects", "sortOrder": 2}, {"optionKey": "C", "content": "Too few people were visiting", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, '"serious problems with damp… the main reason for closing".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000022' WHERE id = '23000000-0000-4000-8000-060000000022';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000022', '23000000-0000-4000-8000-020000000010', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000015', '23000000-0000-4000-8000-0a0000000005', '23000000-0000-4000-8000-070000000022', 1, 1.00);
-- MU2 (KP10, PS-PARA)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000023', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000023', '23000000-0000-4000-8000-060000000023', 1, 'What can visitors see for the first time?', '[{"optionKey": "A", "content": "Ship models that were stored away before", "sortOrder": 1}, {"optionKey": "B", "content": "A new harbour exhibition", "sortOrder": 2}, {"optionKey": "C", "content": "The museum roof garden", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"used to be kept in storage" = stored away before.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000023' WHERE id = '23000000-0000-4000-8000-060000000023';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000023', '23000000-0000-4000-8000-020000000010', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000016', '23000000-0000-4000-8000-0a0000000005', '23000000-0000-4000-8000-070000000023', 2, 1.00);
-- MU4 (KP10, PS-PARA)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000024', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000024', '23000000-0000-4000-8000-060000000024', 1, 'What does the museum plan for next year?', '[{"optionKey": "A", "content": "A cafe on the roof", "sortOrder": 1}, {"optionKey": "B", "content": "More repairs to the roof", "sortOrder": 2}, {"optionKey": "C", "content": "A second building", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"a small cafe on the roof next spring".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000024' WHERE id = '23000000-0000-4000-8000-060000000024';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000024', '23000000-0000-4000-8000-020000000010', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000017', '23000000-0000-4000-8000-0a0000000005', '23000000-0000-4000-8000-070000000024', 3, 1.00);

-- PRACTICE_SET PS-TRAP
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('23000000-0000-4000-8000-080000000006', 'PS-TRAP', 'Luyện thêm PS-TRAP: Hẹn gặp lên kế hoạch', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('23000000-0000-4000-8000-090000000006', '23000000-0000-4000-8000-080000000006', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '23000000-0000-4000-8000-090000000006' WHERE id = '23000000-0000-4000-8000-080000000006';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('23000000-0000-4000-8000-0a0000000006', '23000000-0000-4000-8000-090000000006', 'Hẹn gặp lên kế hoạch', 'LISTENING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('23000000-0000-4000-8000-0c0000000006', '23000000-0000-4000-8000-050000000006', '23000000-0000-4000-8000-0a0000000006', 0);
-- TM1 (KP11, PS-TRAP)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000025', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000025', '23000000-0000-4000-8000-060000000025', 1, 'When will they meet?', '[{"optionKey": "A", "content": "Thursday", "sortOrder": 1}, {"optionKey": "B", "content": "Friday", "sortOrder": 2}, {"optionKey": "C", "content": "Saturday", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Thursday được nhắc trước rồi bị đổi: "Could we make it Friday instead?"', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000025' WHERE id = '23000000-0000-4000-8000-060000000025';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000025', '23000000-0000-4000-8000-020000000011', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000018', '23000000-0000-4000-8000-0a0000000006', '23000000-0000-4000-8000-070000000025', 1, 1.00);
-- TM2 (KP11, PS-TRAP)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000026', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000026', '23000000-0000-4000-8000-060000000026', 1, 'Where will they meet?', '[{"optionKey": "A", "content": "At the station", "sortOrder": 1}, {"optionKey": "B", "content": "In the library", "sortOrder": 2}, {"optionKey": "C", "content": "At a cafe", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Station bị đổi thành "the cafe next to the library". Library là bẫy.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000026' WHERE id = '23000000-0000-4000-8000-060000000026';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000026', '23000000-0000-4000-8000-020000000011', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000019', '23000000-0000-4000-8000-0a0000000006', '23000000-0000-4000-8000-070000000026', 2, 1.00);
-- TM3 (KP11, PS-TRAP)
INSERT INTO questions (id, question_type, skill, status) VALUES ('23000000-0000-4000-8000-060000000027', 'MULTIPLE_CHOICE', 'LISTENING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('23000000-0000-4000-8000-070000000027', '23000000-0000-4000-8000-060000000027', 1, 'What will Anna bring?', '[{"optionKey": "A", "content": "Sandwiches", "sortOrder": 1}, {"optionKey": "B", "content": "The map", "sortOrder": 2}, {"optionKey": "C", "content": "Lunch for both", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Sandwiches bị gạt đi: "No need… Just bring the map."', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '23000000-0000-4000-8000-070000000027' WHERE id = '23000000-0000-4000-8000-060000000027';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('23000000-0000-4000-8000-070000000027', '23000000-0000-4000-8000-020000000011', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('23000000-0000-4000-8000-0b0000000020', '23000000-0000-4000-8000-0a0000000006', '23000000-0000-4000-8000-070000000027', 3, 1.00);
