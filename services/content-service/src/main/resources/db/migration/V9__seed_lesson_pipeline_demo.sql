-- Demo curriculum for the lesson learning flow: lessons L1-L4 and TF1, two final-test codes for DEMO_READING
-- (X1, X2), one for TFNG_SKILLS (X5) and six practice sets for review. Generated from the MVP seed content;
-- every question has an answer spec and an explanation. KP5 deliberately has no practice set.

INSERT INTO topics (id, code, name, sort_order, status) VALUES
    ('20000000-0000-4000-8000-010000000001', 'TFNG_SKILLS', 'True / False / Not Given', 910, 'ACTIVE');

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('20000000-0000-4000-8000-020000000002', '10000000-0000-4000-8000-000000000001', 'DR_IDEA_OR_DETAIL', 'Ý chính hay chi tiết', 'STRATEGY', 'PROCEDURE', 'READING', 'Phân biệt nhận định chung với số liệu, ví dụ dùng để chứng minh.', 'ACTIVE'),
    ('20000000-0000-4000-8000-020000000003', '10000000-0000-4000-8000-000000000001', 'DR_TOPIC_SENTENCE', 'Câu chủ đề', 'STRATEGY', 'PROCEDURE', 'READING', 'Tìm câu nêu ý mà cả đoạn triển khai.', 'ACTIVE'),
    ('20000000-0000-4000-8000-020000000004', '10000000-0000-4000-8000-000000000001', 'DR_MATCHING_HEADINGS', 'Chọn tiêu đề đoạn', 'STRATEGY', 'PROCEDURE', 'READING', 'Chọn tiêu đề khớp ý chính của cả đoạn.', 'ACTIVE'),
    ('20000000-0000-4000-8000-020000000005', '20000000-0000-4000-8000-010000000001', 'TFNG_FALSE_VS_NOT_GIVEN', 'False hay Not Given', 'STRATEGY', 'PROCEDURE', 'READING', 'Phân biệt thông tin bị nói ngược lại với thông tin không được nhắc tới.', 'ACTIVE');

-- Reading passages
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000001', 'PASSAGE', 'A. Across Europe and North America, city planners are turning to green roofs to cope with hotter summers. A green roof is a layer of soil and plants laid over a waterproof membrane on top of a building.

B. The most important benefit of green roofs is that they keep buildings cool. On a summer afternoon, a conventional black roof can reach 80°C, while a planted roof nearby rarely rises above 30°C. As a result, the floors below need far less air conditioning.

C. Green roofs also manage rainwater. The soil soaks up much of a heavy shower and releases it slowly, which takes pressure off city drains. In Copenhagen, new flat roofs must now be planted for this reason.

D. Not everyone is convinced. Critics point out that green roofs are expensive to install and need regular care, and that many older buildings are not strong enough to carry the extra weight.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000002', 'PASSAGE', 'A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.

B. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.

C. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000003', 'PASSAGE', 'A. Recycling plastic is harder than most people think. Only about 9% of all plastic ever made has been recycled.

B. One reason is that there are many types of plastic, and most recycling plants can process only a few. Yoghurt pots and drink bottles, for example, often need different machines.

C. New chemical methods may help. They break plastic down into its basic building blocks, which can then be made into new plastic of high quality.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000004', 'PASSAGE', 'A. The Hillside community garden opened in 2015 on land that had been an empty car park.

B. Members pay a small yearly fee and can borrow the tools that are kept in the shed.

C. The garden is open from 7 a.m. to 8 p.m. between April and September.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000005', 'PASSAGE', 'A. Beekeeping is no longer only a country pursuit. In London, Paris and New York, thousands of hives now sit on rooftops and in backyards, kept by office workers, schools and even hotels.

B. Supporters say city bees do well because parks and gardens offer a wide variety of flowers throughout the year. Honey from urban hives often wins prizes for its complex flavour.

C. However, scientists warn that too many hives can harm wild bees. When honeybees are crowded into a small area, they compete with native species for the same limited flowers.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000006', 'PASSAGE', 'A. Several companies have tested a four-day working week, paying staff the same salary for fewer hours.

B. In most trials, managers reported that productivity stayed the same or even rose, while employees said they felt less stressed and took fewer sick days.

C. Critics, however, point out that the model suits office work better than hospitals or factories, where someone must be present every day.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000007', 'PASSAGE', 'A. Sleep plays a key role in learning. During deep sleep, the brain replays what happened during the day and strengthens important memories.

B. In one experiment, students who slept after studying a list of words remembered 20% more of them the next morning than students who stayed awake.

C. Short naps can help too. A nap of just 30 minutes improved performance on a drawing task in a 2018 study.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000008', 'PASSAGE', 'A. Copenhagen has become one of the easiest cities in the world to cycle in. Over 60% of residents ride to work or school every day.

B. The city invested heavily in safety. Its cycle lanes are separated from traffic by a raised kerb, and some bridges are built only for bicycles.

C. Cycling also saves money. Officials estimate that every kilometre cycled saves the health system about one euro.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000009', 'PASSAGE', 'A. Desert plants have developed clever ways to survive with little water. Many store water in thick stems. Others have tiny leaves that lose less moisture.

B. Some plants avoid the dry season altogether. Their seeds lie in the sand for years and sprout only after heavy rain. The plants then flower and produce new seeds within a few weeks.

C. Roots matter as much as leaves. The mesquite tree sends roots over 50 metres deep to reach underground water. Cacti, in contrast, spread shallow roots wide to catch light rain.

D. Animals benefit from these plants. Birds nest in tall cacti, and lizards shelter under desert shrubs during the hottest hours.', 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('20000000-0000-4000-8000-050000000010', 'PASSAGE', 'A. Coffee was first drunk in Yemen in the 15th century, and from there it spread to Turkey, Europe and the Americas.

B. Today coffee is one of the world''s most traded products. Brazil alone produces about a third of the global supply.

C. Climate change threatens coffee farms. Rising temperatures and new plant diseases could halve the land suitable for growing coffee by 2050.

D. Researchers are testing wild coffee species that survive heat better, hoping to protect future harvests.', 'VALID');

-- Lesson questions
-- Q13 (KP3, L1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000001', '20000000-0000-4000-8000-060000000001', 1, 'Which sentence is the topic sentence of paragraph B?', '[{"optionKey": "A", "content": "The most important benefit of green roofs is that they keep buildings cool.", "sortOrder": 1}, {"optionKey": "B", "content": "On a summer afternoon, a conventional black roof can reach 80°C…", "sortOrder": 2}, {"optionKey": "C", "content": "As a result, the floors below need far less air conditioning.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Câu A nêu ý của cả đoạn (làm mát). Hai câu sau là số liệu và hệ quả để chứng minh.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000001' WHERE id = '20000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000001', '20000000-0000-4000-8000-020000000003', 1.00);
-- Q1 (KP3, L1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000002', '20000000-0000-4000-8000-060000000002', 1, 'Which sentence is the topic sentence of paragraph C?', '[{"optionKey": "A", "content": "Green roofs also manage rainwater.", "sortOrder": 1}, {"optionKey": "B", "content": "The soil soaks up much of a heavy shower and releases it slowly…", "sortOrder": 2}, {"optionKey": "C", "content": "In Copenhagen, new flat roofs must now be planted for this reason.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"Green roofs also manage rainwater" nêu chủ đề. Câu B giải thích cách làm, câu C là ví dụ.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000002' WHERE id = '20000000-0000-4000-8000-060000000002';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000002', '20000000-0000-4000-8000-020000000003', 1.00);
-- Q11 (KP3, L1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000003', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000003', '20000000-0000-4000-8000-060000000003', 1, 'Which sentence tells you what paragraph D is about?', '[{"optionKey": "A", "content": "Not everyone is convinced.", "sortOrder": 1}, {"optionKey": "B", "content": "Critics point out that green roofs are expensive to install and need regular care…", "sortOrder": 2}, {"optionKey": "C", "content": "…many older buildings are not strong enough to carry the extra weight.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"Not everyone is convinced" báo trước cả đoạn nói về ý kiến phản đối. Câu B chỉ là một lý do cụ thể.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000003' WHERE id = '20000000-0000-4000-8000-060000000003';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000003', '20000000-0000-4000-8000-020000000003', 1.00);
-- Q12 (KP3, L1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000004', 'FILL_IN_BLANK', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000004', '20000000-0000-4000-8000-060000000004', 1, 'Complete with ONE WORD from paragraph D: people who doubt green roofs are called ______.', NULL, '{"type": "FILL", "accepted": ["critics"]}'::jsonb, 1, 'Đoạn D: "Critics point out that…". Không phân biệt hoa thường.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000004' WHERE id = '20000000-0000-4000-8000-060000000004';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000004', '20000000-0000-4000-8000-020000000003', 1.00);
-- Q5 (KP1, L2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000005', '20000000-0000-4000-8000-060000000005', 1, 'What is the main idea of the whole passage?', '[{"optionKey": "A", "content": "Black roofs get much hotter than planted roofs.", "sortOrder": 1}, {"optionKey": "B", "content": "Green roofs bring cities real benefits but also have drawbacks.", "sortOrder": 2}, {"optionKey": "C", "content": "Copenhagen requires new flat roofs to be planted.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Bài nói lợi ích (làm mát, thoát nước) và nhược điểm (đoạn D). A và C chỉ đúng với một chi tiết.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000005' WHERE id = '20000000-0000-4000-8000-060000000005';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000005', '10000000-0000-4000-8000-000000000002', 1.00);
-- Q3 (KP2, L3)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000006', '20000000-0000-4000-8000-060000000006', 1, 'In paragraph B, "a conventional black roof can reach 80°C" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Con số 80°C là bằng chứng cho ý chính "green roofs keep buildings cool".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000006' WHERE id = '20000000-0000-4000-8000-060000000006';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000006', '20000000-0000-4000-8000-020000000002', 1.00);
-- Q4 (KP4, L4)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000007', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000007', '20000000-0000-4000-8000-060000000007', 1, 'Choose the best heading for paragraph C.', '[{"optionKey": "i", "content": "Cooling from above", "sortOrder": 1}, {"optionKey": "ii", "content": "Where the rain goes", "sortOrder": 2}, {"optionKey": "iii", "content": "Too heavy for old buildings", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "ii"}'::jsonb, 1, 'Đoạn C nói về nước mưa. Heading i hợp đoạn B, iii hợp đoạn D.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000007' WHERE id = '20000000-0000-4000-8000-060000000007';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000007', '20000000-0000-4000-8000-020000000004', 1.00);
-- QT1 (KP5, TF1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000008', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000008', '20000000-0000-4000-8000-060000000008', 1, 'Passage: "The museum opens at 9 a.m. on weekdays." Statement: "The museum opens at 9 a.m. on Saturdays."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'Đoạn văn chỉ nói ngày thường, không nói gì về thứ Bảy: NOT GIVEN.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000008' WHERE id = '20000000-0000-4000-8000-060000000008';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000008', '20000000-0000-4000-8000-020000000005', 1.00);

-- Lessons and blocks
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('20000000-0000-4000-8000-030000000001', '10000000-0000-4000-8000-000000000001', 'L1', 'Câu chủ đề nằm ở đâu', 1, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000001', '20000000-0000-4000-8000-020000000003');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000001', '20000000-0000-4000-8000-030000000001', 1, 'TEXT', 'Câu chủ đề (topic sentence) nêu ý mà cả đoạn triển khai. Trong bài IELTS nó thường là câu đầu đoạn, đôi khi là câu thứ hai sau một câu dẫn. Mẹo: thử bỏ câu đó đi. Nếu đoạn văn mất ý chung thì đó là câu chủ đề.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('20000000-0000-4000-8000-040000000002', '20000000-0000-4000-8000-030000000001', 2, 'ASSET', '20000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000003', '20000000-0000-4000-8000-030000000001', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000003', '20000000-0000-4000-8000-070000000001', 1);
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000004', '20000000-0000-4000-8000-030000000001', 4, 'TEXT', 'Luyện thêm với đoạn C và D.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000005', '20000000-0000-4000-8000-030000000001', 5, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000005', '20000000-0000-4000-8000-070000000002', 1);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000005', '20000000-0000-4000-8000-070000000003', 2);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000005', '20000000-0000-4000-8000-070000000004', 3);
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('20000000-0000-4000-8000-030000000002', '10000000-0000-4000-8000-000000000001', 'L2', 'Ý chính của cả bài', 2, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000002', '10000000-0000-4000-8000-000000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000006', '20000000-0000-4000-8000-030000000002', 1, 'TEXT', 'Ý chính của cả bài là điều mọi đoạn cùng góp vào. Đọc câu chủ đề của từng đoạn rồi tìm điểm chung. Mẹo: Đáp án đúng thường khái quát; đáp án bẫy chỉ đúng với một đoạn.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('20000000-0000-4000-8000-040000000007', '20000000-0000-4000-8000-030000000002', 2, 'ASSET', '20000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000008', '20000000-0000-4000-8000-030000000002', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000008', '20000000-0000-4000-8000-070000000005', 1);
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('20000000-0000-4000-8000-030000000003', '10000000-0000-4000-8000-000000000001', 'L3', 'Ý chính hay chi tiết?', 3, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000003', '20000000-0000-4000-8000-020000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000009', '20000000-0000-4000-8000-030000000003', 1, 'TEXT', 'Ý chính là nhận định chung. Chi tiết là số liệu, ví dụ, tên riêng, kết quả nghiên cứu dùng để chứng minh. Mẹo: Hỏi: câu này trả lời "điều gì" hay "bằng chứng nào"?');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('20000000-0000-4000-8000-040000000010', '20000000-0000-4000-8000-030000000003', 2, 'ASSET', '20000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000011', '20000000-0000-4000-8000-030000000003', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000011', '20000000-0000-4000-8000-070000000006', 1);
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('20000000-0000-4000-8000-030000000004', '10000000-0000-4000-8000-000000000001', 'L4', 'Dạng Matching Headings', 4, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000004', '20000000-0000-4000-8000-020000000004');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000004', '10000000-0000-4000-8000-000000000002');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000012', '20000000-0000-4000-8000-030000000004', 1, 'TEXT', 'Tiêu đề phải khớp ý chính của cả đoạn, không phải một chi tiết trong đoạn. Tìm câu chủ đề trước, rồi chọn tiêu đề diễn đạt lại nó.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, asset_id) VALUES ('20000000-0000-4000-8000-040000000013', '20000000-0000-4000-8000-030000000004', 2, 'ASSET', '20000000-0000-4000-8000-050000000001');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000014', '20000000-0000-4000-8000-030000000004', 3, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000014', '20000000-0000-4000-8000-070000000007', 1);
INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES ('20000000-0000-4000-8000-030000000005', '20000000-0000-4000-8000-010000000001', 'TF1', 'False hay Not Given?', 1, 'PUBLISHED');
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000005', '20000000-0000-4000-8000-020000000005');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES ('20000000-0000-4000-8000-040000000015', '20000000-0000-4000-8000-030000000005', 1, 'TEXT', 'TRUE: đoạn văn nói giống. FALSE: nói ngược lại. NOT GIVEN: không nói tới. Mẹo: Bẫy hay gặp: chọn FALSE khi thông tin chỉ đơn giản là không có.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES ('20000000-0000-4000-8000-040000000016', '20000000-0000-4000-8000-030000000005', 2, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES ('20000000-0000-4000-8000-040000000016', '20000000-0000-4000-8000-070000000008', 1);

-- TOPIC_TEST X1
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000001', 'X1', 'Đề cuối X1: Street trees', 'TOPIC_TEST', '10000000-0000-4000-8000-000000000001', 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000001', '20000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000001' WHERE id = '20000000-0000-4000-8000-080000000001';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000001', '20000000-0000-4000-8000-090000000001', 'Street trees', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000001', '20000000-0000-4000-8000-050000000002', '20000000-0000-4000-8000-0a0000000001', 0);
-- Q2 (KP1, X1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000009', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000009', '20000000-0000-4000-8000-060000000009', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "How oak trees grow in cities", "sortOrder": 1}, {"optionKey": "B", "content": "The ways street trees improve city life", "sortOrder": 2}, {"optionKey": "C", "content": "A 2019 study of stress in Toronto", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Cả ba đoạn cùng nói lợi ích của cây đường phố.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000009' WHERE id = '20000000-0000-4000-8000-060000000009';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000009', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000001', '20000000-0000-4000-8000-0a0000000001', '20000000-0000-4000-8000-070000000009', 1, 1.00);
-- Q14 (KP3, X1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000010', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000010', '20000000-0000-4000-8000-060000000010', 1, 'Which sentence is the topic sentence of paragraph B?', '[{"optionKey": "A", "content": "Trees filter the air.", "sortOrder": 1}, {"optionKey": "B", "content": "Their leaves trap fine dust from traffic…", "sortOrder": 2}, {"optionKey": "C", "content": "…a single mature oak can remove several kilograms of pollutants a year.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"Trees filter the air." là ý khái quát; hai câu sau là cách lọc và số liệu.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000010' WHERE id = '20000000-0000-4000-8000-060000000010';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000010', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000002', '20000000-0000-4000-8000-0a0000000001', '20000000-0000-4000-8000-070000000010', 2, 1.00);
-- Q15 (KP2, X1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000011', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000011', '20000000-0000-4000-8000-060000000011', 1, 'In paragraph C, "residents of tree-lined streets reported lower stress" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Kết quả nghiên cứu ở Toronto là bằng chứng cho ý "Trees also calm people".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000011' WHERE id = '20000000-0000-4000-8000-060000000011';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000011', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000003', '20000000-0000-4000-8000-0a0000000001', '20000000-0000-4000-8000-070000000011', 3, 1.00);
-- Q16 (KP4, X1)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000012', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000012', '20000000-0000-4000-8000-060000000012', 1, 'Choose the best heading for paragraph C.', '[{"optionKey": "i", "content": "Cleaner air", "sortOrder": 1}, {"optionKey": "ii", "content": "A calmer mind", "sortOrder": 2}, {"optionKey": "iii", "content": "The cost of planting", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "ii"}'::jsonb, 1, 'Đoạn C nói cây giúp giảm căng thẳng.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000012' WHERE id = '20000000-0000-4000-8000-060000000012';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000012', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000004', '20000000-0000-4000-8000-0a0000000001', '20000000-0000-4000-8000-070000000012', 4, 1.00);

-- TOPIC_TEST X2
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000002', 'X2', 'Đề cuối X2: Recycling plastic', 'TOPIC_TEST', '10000000-0000-4000-8000-000000000001', 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000002', '20000000-0000-4000-8000-080000000002', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000002' WHERE id = '20000000-0000-4000-8000-080000000002';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000002', '20000000-0000-4000-8000-090000000002', 'Recycling plastic', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000002', '20000000-0000-4000-8000-050000000003', '20000000-0000-4000-8000-0a0000000002', 0);
-- PL1 (KP1, X2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000013', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000013', '20000000-0000-4000-8000-060000000013', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "Why plastic recycling is difficult and how it may improve", "sortOrder": 1}, {"optionKey": "B", "content": "How yoghurt pots are made", "sortOrder": 2}, {"optionKey": "C", "content": "The history of plastic", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Đoạn A–B nói vì sao khó, đoạn C nói cách cải thiện.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000013' WHERE id = '20000000-0000-4000-8000-060000000013';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000013', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000005', '20000000-0000-4000-8000-0a0000000002', '20000000-0000-4000-8000-070000000013', 1, 1.00);
-- PL2 (KP3, X2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000014', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000014', '20000000-0000-4000-8000-060000000014', 1, 'Which sentence is the topic sentence of paragraph B?', '[{"optionKey": "A", "content": "One reason is that there are many types of plastic, and most recycling plants can process only a few.", "sortOrder": 1}, {"optionKey": "B", "content": "Yoghurt pots and drink bottles, for example, often need different machines.", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Câu B mở đầu bằng "for example", tức là ví dụ cho câu A.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000014' WHERE id = '20000000-0000-4000-8000-060000000014';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000014', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000006', '20000000-0000-4000-8000-0a0000000002', '20000000-0000-4000-8000-070000000014', 2, 1.00);
-- PL3 (KP2, X2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000015', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000015', '20000000-0000-4000-8000-060000000015', 1, '"Only about 9% of all plastic ever made has been recycled" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Con số 9% chứng minh ý "Recycling plastic is harder than most people think".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000015' WHERE id = '20000000-0000-4000-8000-060000000015';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000015', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000007', '20000000-0000-4000-8000-0a0000000002', '20000000-0000-4000-8000-070000000015', 3, 1.00);
-- PL4 (KP4, X2)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000016', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000016', '20000000-0000-4000-8000-060000000016', 1, 'Choose the best heading for paragraph C.', '[{"optionKey": "i", "content": "Too many kinds", "sortOrder": 1}, {"optionKey": "ii", "content": "A chemical solution", "sortOrder": 2}, {"optionKey": "iii", "content": "Plastic in the ocean", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "ii"}'::jsonb, 1, 'Đoạn C nói phương pháp hóa học mới.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000016' WHERE id = '20000000-0000-4000-8000-060000000016';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000016', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000008', '20000000-0000-4000-8000-0a0000000002', '20000000-0000-4000-8000-070000000016', 4, 1.00);

-- TOPIC_TEST X5
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000003', 'X5', 'Đề cuối X5: Hillside community garden', 'TOPIC_TEST', '20000000-0000-4000-8000-010000000001', 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000003', '20000000-0000-4000-8000-080000000003', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000003' WHERE id = '20000000-0000-4000-8000-080000000003';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000003', '20000000-0000-4000-8000-090000000003', 'Hillside community garden', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000003', '20000000-0000-4000-8000-050000000004', '20000000-0000-4000-8000-0a0000000003', 0);
-- TT1 (KP5, X5)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000017', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000017', '20000000-0000-4000-8000-060000000017', 1, 'Statement: "The garden was built on land that used to be a car park."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "TRUE"}'::jsonb, 1, 'Đoạn A: "on land that had been an empty car park".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000017' WHERE id = '20000000-0000-4000-8000-060000000017';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000017', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000009', '20000000-0000-4000-8000-0a0000000003', '20000000-0000-4000-8000-070000000017', 1, 1.00);
-- TT2 (KP5, X5)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000018', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000018', '20000000-0000-4000-8000-060000000018', 1, 'Statement: "Members have to bring their own tools."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Đoạn B nói ngược lại: thành viên mượn dụng cụ để trong kho.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000018' WHERE id = '20000000-0000-4000-8000-060000000018';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000018', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000010', '20000000-0000-4000-8000-0a0000000003', '20000000-0000-4000-8000-070000000018', 2, 1.00);
-- TT3 (KP5, X5)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000019', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000019', '20000000-0000-4000-8000-060000000019', 1, 'Statement: "Most members live within walking distance of the garden."', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'Đoạn văn không nói thành viên sống ở đâu.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000019' WHERE id = '20000000-0000-4000-8000-060000000019';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000019', '20000000-0000-4000-8000-020000000005', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000011', '20000000-0000-4000-8000-0a0000000003', '20000000-0000-4000-8000-070000000019', 3, 1.00);

-- PRACTICE_SET PS-KP1-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000004', 'PS-KP1-A', 'Luyện thêm PS-KP1-A: Bees in the city', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000004', '20000000-0000-4000-8000-080000000004', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000004' WHERE id = '20000000-0000-4000-8000-080000000004';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000004', '20000000-0000-4000-8000-090000000004', 'Bees in the city', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000004', '20000000-0000-4000-8000-050000000005', '20000000-0000-4000-8000-0a0000000004', 0);
-- BE1 (KP1, PS-KP1-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000020', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000020', '20000000-0000-4000-8000-060000000020', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "City honey tastes better than country honey", "sortOrder": 1}, {"optionKey": "B", "content": "Urban beekeeping is growing, with benefits and risks", "sortOrder": 2}, {"optionKey": "C", "content": "Wild bees are disappearing from London", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Đoạn A: đang phát triển; B: lợi ích; C: rủi ro.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000020' WHERE id = '20000000-0000-4000-8000-060000000020';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000020', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000012', '20000000-0000-4000-8000-0a0000000004', '20000000-0000-4000-8000-070000000020', 1, 1.00);
-- BE2 (KP1, PS-KP1-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000021', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000021', '20000000-0000-4000-8000-060000000021', 1, 'What is the main idea of paragraph B?', '[{"optionKey": "A", "content": "Why city bees can do well", "sortOrder": 1}, {"optionKey": "B", "content": "How to win a honey prize", "sortOrder": 2}, {"optionKey": "C", "content": "Where parks are located", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Cả đoạn giải thích vì sao ong thành phố phát triển tốt.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000021' WHERE id = '20000000-0000-4000-8000-060000000021';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000021', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000013', '20000000-0000-4000-8000-0a0000000004', '20000000-0000-4000-8000-070000000021', 2, 1.00);
-- BE3 (KP1, PS-KP1-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000022', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000022', '20000000-0000-4000-8000-060000000022', 1, 'What is the main idea of paragraph C?', '[{"optionKey": "A", "content": "A possible downside of city hives", "sortOrder": 1}, {"optionKey": "B", "content": "How scientists count bees", "sortOrder": 2}, {"optionKey": "C", "content": "Why flowers are limited", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, '"However… can harm wild bees" là mặt trái.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000022' WHERE id = '20000000-0000-4000-8000-060000000022';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000022', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000014', '20000000-0000-4000-8000-0a0000000004', '20000000-0000-4000-8000-070000000022', 3, 1.00);
-- BE4 (KP1, PS-KP1-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000023', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000023', '20000000-0000-4000-8000-060000000023', 1, 'Which title best fits the whole passage?', '[{"optionKey": "A", "content": "Buzz in the City: Promise and Problems", "sortOrder": 1}, {"optionKey": "B", "content": "A Guide to Building Hives", "sortOrder": 2}, {"optionKey": "C", "content": "The History of Honey", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Tiêu đề phải bao cả lợi ích lẫn rủi ro.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000023' WHERE id = '20000000-0000-4000-8000-060000000023';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000023', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000015', '20000000-0000-4000-8000-0a0000000004', '20000000-0000-4000-8000-070000000023', 4, 1.00);

-- PRACTICE_SET PS-KP1-B
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000005', 'PS-KP1-B', 'Luyện thêm PS-KP1-B: The four-day week', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000005', '20000000-0000-4000-8000-080000000005', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000005' WHERE id = '20000000-0000-4000-8000-080000000005';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000005', '20000000-0000-4000-8000-090000000005', 'The four-day week', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000005', '20000000-0000-4000-8000-050000000006', '20000000-0000-4000-8000-0a0000000005', 0);
-- WW1 (KP1, PS-KP1-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000024', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000024', '20000000-0000-4000-8000-060000000024', 1, 'What is the passage mainly about?', '[{"optionKey": "A", "content": "Four-day weeks save companies money", "sortOrder": 1}, {"optionKey": "B", "content": "Four-day week trials show gains but may not suit every job", "sortOrder": 2}, {"optionKey": "C", "content": "Hospitals need more staff", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Đoạn B: kết quả tốt; đoạn C: giới hạn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000024' WHERE id = '20000000-0000-4000-8000-060000000024';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000024', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000016', '20000000-0000-4000-8000-0a0000000005', '20000000-0000-4000-8000-070000000024', 1, 1.00);
-- WW2 (KP1, PS-KP1-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000025', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000025', '20000000-0000-4000-8000-060000000025', 1, 'What is the main idea of paragraph B?', '[{"optionKey": "A", "content": "Positive results of the trials", "sortOrder": 1}, {"optionKey": "B", "content": "How sick days are counted", "sortOrder": 2}, {"optionKey": "C", "content": "Why managers dislike change", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Năng suất giữ nguyên hoặc tăng, nhân viên bớt căng thẳng.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000025' WHERE id = '20000000-0000-4000-8000-060000000025';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000025', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000017', '20000000-0000-4000-8000-0a0000000005', '20000000-0000-4000-8000-070000000025', 2, 1.00);
-- WW3 (KP1, PS-KP1-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000026', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000026', '20000000-0000-4000-8000-060000000026', 1, 'What is the main idea of paragraph C?', '[{"optionKey": "A", "content": "Limits of the four-day model", "sortOrder": 1}, {"optionKey": "B", "content": "How factories are designed", "sortOrder": 2}, {"optionKey": "C", "content": "Salaries in hospitals", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Mô hình hợp văn phòng hơn bệnh viện, nhà máy.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000026' WHERE id = '20000000-0000-4000-8000-060000000026';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000026', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000018', '20000000-0000-4000-8000-0a0000000005', '20000000-0000-4000-8000-070000000026', 3, 1.00);
-- WW4 (KP1, PS-KP1-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000027', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000027', '20000000-0000-4000-8000-060000000027', 1, 'Which title best fits the whole passage?', '[{"optionKey": "A", "content": "Less Time, Same Work?", "sortOrder": 1}, {"optionKey": "B", "content": "Factory Life Today", "sortOrder": 2}, {"optionKey": "C", "content": "A History of the Weekend", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Bao được cả thử nghiệm lẫn câu hỏi còn mở.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000027' WHERE id = '20000000-0000-4000-8000-060000000027';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000027', '10000000-0000-4000-8000-000000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000019', '20000000-0000-4000-8000-0a0000000005', '20000000-0000-4000-8000-070000000027', 4, 1.00);

-- PRACTICE_SET PS-KP2-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000006', 'PS-KP2-A', 'Luyện thêm PS-KP2-A: Sleep and memory', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000006', '20000000-0000-4000-8000-080000000006', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000006' WHERE id = '20000000-0000-4000-8000-080000000006';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000006', '20000000-0000-4000-8000-090000000006', 'Sleep and memory', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000006', '20000000-0000-4000-8000-050000000007', '20000000-0000-4000-8000-0a0000000006', 0);
-- SL1 (KP2, PS-KP2-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000028', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000028', '20000000-0000-4000-8000-060000000028', 1, '"Sleep plays a key role in learning." is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "MAIN_IDEA"}'::jsonb, 1, 'Nhận định chung, cả đoạn A triển khai nó.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000028' WHERE id = '20000000-0000-4000-8000-060000000028';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000028', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000020', '20000000-0000-4000-8000-0a0000000006', '20000000-0000-4000-8000-070000000028', 1, 1.00);
-- SL2 (KP2, PS-KP2-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000029', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000029', '20000000-0000-4000-8000-060000000029', 1, '"students who slept… remembered 20% more" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Kết quả thí nghiệm, dùng làm bằng chứng.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000029' WHERE id = '20000000-0000-4000-8000-060000000029';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000029', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000021', '20000000-0000-4000-8000-0a0000000006', '20000000-0000-4000-8000-070000000029', 2, 1.00);
-- SL3 (KP2, PS-KP2-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000030', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000030', '20000000-0000-4000-8000-060000000030', 1, '"Short naps can help too." is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "MAIN_IDEA"}'::jsonb, 1, 'Nhận định chung của đoạn C.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000030' WHERE id = '20000000-0000-4000-8000-060000000030';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000030', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000022', '20000000-0000-4000-8000-0a0000000006', '20000000-0000-4000-8000-070000000030', 3, 1.00);
-- SL4 (KP2, PS-KP2-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000031', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000031', '20000000-0000-4000-8000-060000000031', 1, '"A nap of just 30 minutes improved performance on a drawing task" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Kết quả nghiên cứu cụ thể năm 2018.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000031' WHERE id = '20000000-0000-4000-8000-060000000031';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000031', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000023', '20000000-0000-4000-8000-0a0000000006', '20000000-0000-4000-8000-070000000031', 4, 1.00);

-- PRACTICE_SET PS-KP2-B
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000007', 'PS-KP2-B', 'Luyện thêm PS-KP2-B: Cycling in Copenhagen', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000007', '20000000-0000-4000-8000-080000000007', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000007' WHERE id = '20000000-0000-4000-8000-080000000007';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000007', '20000000-0000-4000-8000-090000000007', 'Cycling in Copenhagen', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000007', '20000000-0000-4000-8000-050000000008', '20000000-0000-4000-8000-0a0000000007', 0);
-- CB1 (KP2, PS-KP2-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000032', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000032', '20000000-0000-4000-8000-060000000032', 1, '"Copenhagen has become one of the easiest cities in the world to cycle in." is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "MAIN_IDEA"}'::jsonb, 1, 'Nhận định chung của đoạn A.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000032' WHERE id = '20000000-0000-4000-8000-060000000032';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000032', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000024', '20000000-0000-4000-8000-0a0000000007', '20000000-0000-4000-8000-070000000032', 1, 1.00);
-- CB2 (KP2, PS-KP2-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000033', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000033', '20000000-0000-4000-8000-060000000033', 1, '"Over 60% of residents ride to work or school every day." is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Số liệu minh họa.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000033' WHERE id = '20000000-0000-4000-8000-060000000033';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000033', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000025', '20000000-0000-4000-8000-0a0000000007', '20000000-0000-4000-8000-070000000033', 2, 1.00);
-- CB3 (KP2, PS-KP2-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000034', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000034', '20000000-0000-4000-8000-060000000034', 1, '"some bridges are built only for bicycles" is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb, 1, 'Ví dụ cho việc đầu tư an toàn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000034' WHERE id = '20000000-0000-4000-8000-060000000034';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000034', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000026', '20000000-0000-4000-8000-0a0000000007', '20000000-0000-4000-8000-070000000034', 3, 1.00);
-- CB4 (KP2, PS-KP2-B)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000035', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000035', '20000000-0000-4000-8000-060000000035', 1, '"Cycling also saves money." is…', '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb, '{"type": "CHOICE", "correct": "MAIN_IDEA"}'::jsonb, 1, 'Nhận định chung của đoạn C.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000035' WHERE id = '20000000-0000-4000-8000-060000000035';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000035', '20000000-0000-4000-8000-020000000002', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000027', '20000000-0000-4000-8000-0a0000000007', '20000000-0000-4000-8000-070000000035', 4, 1.00);

-- PRACTICE_SET PS-KP3-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000008', 'PS-KP3-A', 'Luyện thêm PS-KP3-A: Desert plants', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000008', '20000000-0000-4000-8000-080000000008', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000008' WHERE id = '20000000-0000-4000-8000-080000000008';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000008', '20000000-0000-4000-8000-090000000008', 'Desert plants', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000008', '20000000-0000-4000-8000-050000000009', '20000000-0000-4000-8000-0a0000000008', 0);
-- DS1 (KP3, PS-KP3-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000036', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000036', '20000000-0000-4000-8000-060000000036', 1, 'Which sentence is the topic sentence of paragraph A?', '[{"optionKey": "A", "content": "Desert plants have developed clever ways to survive with little water.", "sortOrder": 1}, {"optionKey": "B", "content": "Many store water in thick stems.", "sortOrder": 2}, {"optionKey": "C", "content": "Others have tiny leaves that lose less moisture.", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Hai câu sau là hai "cách" cụ thể.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000036' WHERE id = '20000000-0000-4000-8000-060000000036';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000036', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000028', '20000000-0000-4000-8000-0a0000000008', '20000000-0000-4000-8000-070000000036', 1, 1.00);
-- DS2 (KP3, PS-KP3-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000037', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000037', '20000000-0000-4000-8000-060000000037', 1, 'Which sentence is the topic sentence of paragraph B?', '[{"optionKey": "A", "content": "Some plants avoid the dry season altogether.", "sortOrder": 1}, {"optionKey": "B", "content": "Their seeds lie in the sand for years…", "sortOrder": 2}, {"optionKey": "C", "content": "The plants then flower and produce new seeds…", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Các câu sau mô tả cách "tránh mùa khô".', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000037' WHERE id = '20000000-0000-4000-8000-060000000037';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000037', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000029', '20000000-0000-4000-8000-0a0000000008', '20000000-0000-4000-8000-070000000037', 2, 1.00);
-- DS3 (KP3, PS-KP3-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000038', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000038', '20000000-0000-4000-8000-060000000038', 1, 'Which sentence is the topic sentence of paragraph C?', '[{"optionKey": "A", "content": "Roots matter as much as leaves.", "sortOrder": 1}, {"optionKey": "B", "content": "The mesquite tree sends roots over 50 metres deep…", "sortOrder": 2}, {"optionKey": "C", "content": "Cacti, in contrast, spread shallow roots wide…", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Hai câu sau là hai ví dụ về rễ.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000038' WHERE id = '20000000-0000-4000-8000-060000000038';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000038', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000030', '20000000-0000-4000-8000-0a0000000008', '20000000-0000-4000-8000-070000000038', 3, 1.00);
-- DS4 (KP3, PS-KP3-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000039', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000039', '20000000-0000-4000-8000-060000000039', 1, 'Which sentence is the topic sentence of paragraph D?', '[{"optionKey": "A", "content": "Animals benefit from these plants.", "sortOrder": 1}, {"optionKey": "B", "content": "Birds nest in tall cacti…", "sortOrder": 2}, {"optionKey": "C", "content": "…lizards shelter under desert shrubs…", "sortOrder": 3}]'::jsonb, '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Chim và thằn lằn là ví dụ.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000039' WHERE id = '20000000-0000-4000-8000-060000000039';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000039', '20000000-0000-4000-8000-020000000003', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000031', '20000000-0000-4000-8000-0a0000000008', '20000000-0000-4000-8000-070000000039', 4, 1.00);

-- PRACTICE_SET PS-KP4-A
INSERT INTO content_packages (id, code, title, package_type, topic_id, status) VALUES ('20000000-0000-4000-8000-080000000009', 'PS-KP4-A', 'Luyện thêm PS-KP4-A: The story of coffee', 'PRACTICE_SET', NULL, 'PUBLISHED');
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES ('20000000-0000-4000-8000-090000000009', '20000000-0000-4000-8000-080000000009', 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP);
UPDATE content_packages SET current_published_version_id = '20000000-0000-4000-8000-090000000009' WHERE id = '20000000-0000-4000-8000-080000000009';
INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES ('20000000-0000-4000-8000-0a0000000009', '20000000-0000-4000-8000-090000000009', 'The story of coffee', 'READING', 1);
INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES ('20000000-0000-4000-8000-0c0000000009', '20000000-0000-4000-8000-050000000010', '20000000-0000-4000-8000-0a0000000009', 0);
-- CF1 (KP4, PS-KP4-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000040', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000040', '20000000-0000-4000-8000-060000000040', 1, 'Choose the heading for paragraph A.', '[{"optionKey": "i", "content": "A drink that travelled the world", "sortOrder": 1}, {"optionKey": "ii", "content": "A giant global trade", "sortOrder": 2}, {"optionKey": "iii", "content": "A warming threat", "sortOrder": 3}, {"optionKey": "iv", "content": "Searching for tougher plants", "sortOrder": 4}, {"optionKey": "v", "content": "How to brew the perfect cup", "sortOrder": 5}]'::jsonb, '{"type": "CHOICE", "correct": "i"}'::jsonb, 1, 'Đoạn A kể cà phê lan từ Yemen ra thế giới.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000040' WHERE id = '20000000-0000-4000-8000-060000000040';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000040', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000032', '20000000-0000-4000-8000-0a0000000009', '20000000-0000-4000-8000-070000000040', 1, 1.00);
-- CF2 (KP4, PS-KP4-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000041', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000041', '20000000-0000-4000-8000-060000000041', 1, 'Choose the heading for paragraph B.', '[{"optionKey": "i", "content": "A drink that travelled the world", "sortOrder": 1}, {"optionKey": "ii", "content": "A giant global trade", "sortOrder": 2}, {"optionKey": "iii", "content": "A warming threat", "sortOrder": 3}, {"optionKey": "iv", "content": "Searching for tougher plants", "sortOrder": 4}, {"optionKey": "v", "content": "How to brew the perfect cup", "sortOrder": 5}]'::jsonb, '{"type": "CHOICE", "correct": "ii"}'::jsonb, 1, 'Đoạn B nói cà phê là mặt hàng giao dịch lớn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000041' WHERE id = '20000000-0000-4000-8000-060000000041';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000041', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000033', '20000000-0000-4000-8000-0a0000000009', '20000000-0000-4000-8000-070000000041', 2, 1.00);
-- CF3 (KP4, PS-KP4-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000042', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000042', '20000000-0000-4000-8000-060000000042', 1, 'Choose the heading for paragraph C.', '[{"optionKey": "i", "content": "A drink that travelled the world", "sortOrder": 1}, {"optionKey": "ii", "content": "A giant global trade", "sortOrder": 2}, {"optionKey": "iii", "content": "A warming threat", "sortOrder": 3}, {"optionKey": "iv", "content": "Searching for tougher plants", "sortOrder": 4}, {"optionKey": "v", "content": "How to brew the perfect cup", "sortOrder": 5}]'::jsonb, '{"type": "CHOICE", "correct": "iii"}'::jsonb, 1, 'Đoạn C nói biến đổi khí hậu đe dọa.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000042' WHERE id = '20000000-0000-4000-8000-060000000042';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000042', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000034', '20000000-0000-4000-8000-0a0000000009', '20000000-0000-4000-8000-070000000042', 3, 1.00);
-- CF4 (KP4, PS-KP4-A)
INSERT INTO questions (id, question_type, skill, status) VALUES ('20000000-0000-4000-8000-060000000043', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('20000000-0000-4000-8000-070000000043', '20000000-0000-4000-8000-060000000043', 1, 'Choose the heading for paragraph D.', '[{"optionKey": "i", "content": "A drink that travelled the world", "sortOrder": 1}, {"optionKey": "ii", "content": "A giant global trade", "sortOrder": 2}, {"optionKey": "iii", "content": "A warming threat", "sortOrder": 3}, {"optionKey": "iv", "content": "Searching for tougher plants", "sortOrder": 4}, {"optionKey": "v", "content": "How to brew the perfect cup", "sortOrder": 5}]'::jsonb, '{"type": "CHOICE", "correct": "iv"}'::jsonb, 1, 'Đoạn D nói thử giống chịu nóng.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '20000000-0000-4000-8000-070000000043' WHERE id = '20000000-0000-4000-8000-060000000043';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES ('20000000-0000-4000-8000-070000000043', '20000000-0000-4000-8000-020000000004', 1.00);
INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES ('20000000-0000-4000-8000-0b0000000035', '20000000-0000-4000-8000-0a0000000009', '20000000-0000-4000-8000-070000000043', 4, 1.00);
