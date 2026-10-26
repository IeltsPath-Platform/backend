-- IELTS 5.5: the Writing topic becomes the second chapter of the course, right after Demo IELTS Reading, and gets
-- three more Writing lessons so the whole chapter is about writing. The learning service orders a course's free
-- topics by sort_order, so 905 sits between DEMO_READING (900) and TFNG_SKILLS (910). Nothing is deleted: learners'
-- progress on the other topics stays valid and only their position in the course changes.
UPDATE topics SET sort_order = 905, updated_at = CURRENT_TIMESTAMP WHERE code = 'DEMO_WRITING';

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('34000000-0000-4000-8000-020000000001', '25000000-0000-4000-8000-010000000001', 'DEMO_WRITING_TOPIC_SENTENCE',
     'Mở bài và câu chủ đề (Task 2)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Diễn đạt lại đề bài ở mở bài, nêu rõ quan điểm và mở mỗi đoạn thân bài bằng một câu chủ đề nêu ý chính.', 'ACTIVE'),
    ('34000000-0000-4000-8000-020000000002', '25000000-0000-4000-8000-010000000001', 'DEMO_WRITING_LINKING',
     'Từ nối và mạch lạc', 'GRAMMAR', 'CONCEPT', 'WRITING',
     'Dùng từ nối đúng chức năng (đối lập, nguyên nhân, kết quả, bổ sung) để các ý trong bài viết liền mạch.', 'ACTIVE'),
    ('34000000-0000-4000-8000-020000000003', '25000000-0000-4000-8000-010000000001', 'DEMO_WRITING_TREND_LANGUAGE',
     'Từ vựng mô tả xu hướng (Task 1)', 'VOCABULARY', 'MEMORY', 'WRITING',
     'Dùng động từ, danh từ và trạng từ chỉ xu hướng tăng, giảm, giữ nguyên và điểm cao nhất khi mô tả biểu đồ.', 'ACTIVE');

INSERT INTO lessons (id, topic_id, code, title, sort_order, status, estimated_minutes) VALUES
    ('34000000-0000-4000-8000-030000000001', '25000000-0000-4000-8000-010000000001', 'W3',
     'Mở bài và câu chủ đề (Task 2)', 3, 'PUBLISHED', 10),
    ('34000000-0000-4000-8000-030000000002', '25000000-0000-4000-8000-010000000001', 'W4',
     'Từ nối và mạch lạc', 4, 'PUBLISHED', 10),
    ('34000000-0000-4000-8000-030000000003', '25000000-0000-4000-8000-010000000001', 'W5',
     'Mô tả xu hướng (Task 1)', 5, 'PUBLISHED', 10);

INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES
    ('34000000-0000-4000-8000-030000000001', '34000000-0000-4000-8000-020000000001'),
    ('34000000-0000-4000-8000-030000000002', '34000000-0000-4000-8000-020000000002'),
    ('34000000-0000-4000-8000-030000000003', '34000000-0000-4000-8000-020000000003');

-- Each lesson: one theory block, then one exercise block with three questions.
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES
    ('34000000-0000-4000-8000-040000000001', '34000000-0000-4000-8000-030000000001', 1, 'TEXT',
     'Mở bài của Writing Task 2 làm hai việc: diễn đạt lại đề bằng từ ngữ của bạn (không chép nguyên văn) và nêu rõ quan điểm. Mỗi đoạn thân bài mở bằng một câu chủ đề (topic sentence) nói ý chính của cả đoạn: câu này mang tính khái quát, có lý do cốt lõi; ví dụ và số liệu để dành cho các câu sau. Câu chủ đề quá chung chung ("Nhiều người làm việc ở nhà") hoặc là một ví dụ lẻ đều chưa đạt.'),
    ('34000000-0000-4000-8000-040000000003', '34000000-0000-4000-8000-030000000002', 1, 'TEXT',
     'Từ nối giúp người chấm thấy đường đi của lập luận. Chọn theo chức năng: đối lập (however, whereas, on the other hand), nguyên nhân (because, since, due to), kết quả (therefore, consequently, as a result), bổ sung (moreover, in addition). Lưu ý vị trí và dấu câu: "however" đứng đầu câu mới hoặc sau dấu chấm phẩy, còn "because" nối hai mệnh đề trong cùng một câu. Dùng quá nhiều từ nối một cách máy móc cũng làm bài kém tự nhiên.'),
    ('34000000-0000-4000-8000-040000000005', '34000000-0000-4000-8000-030000000003', 1, 'TEXT',
     'Writing Task 1 cần vốn từ chỉ xu hướng chính xác. Tăng: rise, increase, grow, climb. Giảm: fall, decrease, decline, drop. Giữ nguyên: remain stable, stay unchanged, level off. Điểm cao nhất: peak at / reach a peak of. Kết hợp thêm trạng từ chỉ mức độ (sharply, gradually, slightly) và mốc thời gian (between 2010 and 2012) để mô tả đủ ý; tránh lặp đi lặp lại một động từ.');

INSERT INTO lesson_block_knowledge_points (block_id, knowledge_point_id) VALUES
    ('34000000-0000-4000-8000-040000000001', '34000000-0000-4000-8000-020000000001'),
    ('34000000-0000-4000-8000-040000000003', '34000000-0000-4000-8000-020000000002'),
    ('34000000-0000-4000-8000-040000000005', '34000000-0000-4000-8000-020000000003');

INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES
    ('34000000-0000-4000-8000-040000000002', '34000000-0000-4000-8000-030000000001', 2, 'EXERCISE'),
    ('34000000-0000-4000-8000-040000000004', '34000000-0000-4000-8000-030000000002', 2, 'EXERCISE'),
    ('34000000-0000-4000-8000-040000000006', '34000000-0000-4000-8000-030000000003', 2, 'EXERCISE');

-- W3: introduction and topic sentences
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('34000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'WRITING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('34000000-0000-4000-8000-070000000001', '34000000-0000-4000-8000-060000000001', 1,
     'Which sentence is the best topic sentence for a body paragraph about the benefits of working from home?',
     '[{"optionKey": "A", "content": "Many people work from home.", "sortOrder": 1}, {"optionKey": "B", "content": "Working from home benefits employees mainly because it removes the daily commute and gives them more control over their time.", "sortOrder": 2}, {"optionKey": "C", "content": "For example, my cousin saves two hours every day.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'B states the main idea of the paragraph and gives a reason that the rest of the paragraph can develop. A is only a general fact and C is an example, which belongs after the topic sentence.',
     'Câu chủ đề nêu ý chính và lý do cốt lõi; ví dụ đứng sau nó.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000002', '34000000-0000-4000-8000-060000000002', 1,
     'Which introduction sentence paraphrases the task "Some people think children should start school at a very young age" most effectively?',
     '[{"optionKey": "A", "content": "Some people think children should start school at a very young age.", "sortOrder": 1}, {"optionKey": "B", "content": "It is sometimes argued that formal education ought to begin early in a child''s life.", "sortOrder": 2}, {"optionKey": "C", "content": "I will write about schools and children in this essay.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'B keeps the meaning but changes the structure and vocabulary. A copies the question and C does not restate the topic.',
     'Giữ nguyên nghĩa nhưng đổi cấu trúc và từ ngữ so với đề.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000003', '34000000-0000-4000-8000-060000000003', 1,
     'Complete with ONE WORD: in an opinion essay, the thesis statement tells the reader the writer''s ______ on the question.',
     NULL, '{"type": "FILL", "accepted": ["opinion", "position", "view", "stance"]}', 1,
     'The thesis statement states the writer''s opinion (also called position, view or stance) so the reader knows the line of argument.',
     'Từ cần điền nói về điều người viết nghĩ về câu hỏi của đề.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000001' WHERE id = '34000000-0000-4000-8000-060000000001';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000002' WHERE id = '34000000-0000-4000-8000-060000000002';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000003' WHERE id = '34000000-0000-4000-8000-060000000003';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('34000000-0000-4000-8000-070000000001', '34000000-0000-4000-8000-020000000001', 1.0),
    ('34000000-0000-4000-8000-070000000002', '34000000-0000-4000-8000-020000000001', 1.0),
    ('34000000-0000-4000-8000-070000000003', '34000000-0000-4000-8000-020000000001', 1.0);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('34000000-0000-4000-8000-040000000002', '34000000-0000-4000-8000-070000000001', 1),
    ('34000000-0000-4000-8000-040000000002', '34000000-0000-4000-8000-070000000002', 2),
    ('34000000-0000-4000-8000-040000000002', '34000000-0000-4000-8000-070000000003', 3);

-- W4: linking words and coherence
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('34000000-0000-4000-8000-060000000004', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000006', 'FILL_IN_BLANK', 'WRITING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('34000000-0000-4000-8000-070000000004', '34000000-0000-4000-8000-060000000004', 1,
     'Choose the best linking word: "Public transport is cheap; ______, it can be crowded at peak times."',
     '[{"optionKey": "A", "content": "Therefore", "sortOrder": 1}, {"optionKey": "B", "content": "However", "sortOrder": 2}, {"optionKey": "C", "content": "Moreover", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'The second clause contrasts with the first (cheap versus crowded), so a contrast word is needed. Therefore shows result and moreover adds a similar idea.',
     'Hai vế câu nói hai điều trái chiều nhau.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000005', '34000000-0000-4000-8000-060000000005', 1,
     'Which sentence uses a linking word correctly to give a reason?',
     '[{"optionKey": "A", "content": "Many students choose online courses because they are flexible.", "sortOrder": 1}, {"optionKey": "B", "content": "Many students choose online courses although they are flexible.", "sortOrder": 2}, {"optionKey": "C", "content": "Many students choose online courses however they are flexible.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "A"}', 1,
     'Because introduces a reason inside one sentence. Although and however express contrast and do not fit the meaning here.',
     'Tìm từ nối chỉ nguyên nhân, không phải đối lập.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000006', '34000000-0000-4000-8000-060000000006', 1,
     'Complete with ONE WORD that shows a result: "The company reduced its prices; ______, sales rose by 20%."',
     NULL, '{"type": "FILL", "accepted": ["consequently", "therefore", "thus", "hence"]}', 1,
     'Sales rose as an effect of the lower prices, so a result word such as therefore or consequently is needed.',
     'Doanh số tăng là kết quả của việc giảm giá.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000004' WHERE id = '34000000-0000-4000-8000-060000000004';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000005' WHERE id = '34000000-0000-4000-8000-060000000005';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000006' WHERE id = '34000000-0000-4000-8000-060000000006';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('34000000-0000-4000-8000-070000000004', '34000000-0000-4000-8000-020000000002', 1.0),
    ('34000000-0000-4000-8000-070000000005', '34000000-0000-4000-8000-020000000002', 1.0),
    ('34000000-0000-4000-8000-070000000006', '34000000-0000-4000-8000-020000000002', 1.0);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('34000000-0000-4000-8000-040000000004', '34000000-0000-4000-8000-070000000004', 1),
    ('34000000-0000-4000-8000-040000000004', '34000000-0000-4000-8000-070000000005', 2),
    ('34000000-0000-4000-8000-040000000004', '34000000-0000-4000-8000-070000000006', 3);

-- W5: describing trends
INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('34000000-0000-4000-8000-060000000007', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000008', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('34000000-0000-4000-8000-060000000009', 'FILL_IN_BLANK', 'WRITING', 'LEARNING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, status) VALUES
    ('34000000-0000-4000-8000-070000000007', '34000000-0000-4000-8000-060000000007', 1,
     'Sales ______ sharply from 200 to 500 units between 2010 and 2012.',
     '[{"optionKey": "A", "content": "increased", "sortOrder": 1}, {"optionKey": "B", "content": "fell", "sortOrder": 2}, {"optionKey": "C", "content": "remained stable", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "A"}', 1,
     'The figure goes up from 200 to 500, so the verb must express an increase. Fell shows a decrease and remained stable shows no change.',
     'Số liệu đi từ 200 lên 500.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000008', '34000000-0000-4000-8000-060000000008', 1,
     'Which phrase describes a figure that stayed at the same level?',
     '[{"optionKey": "A", "content": "fluctuated wildly", "sortOrder": 1}, {"optionKey": "B", "content": "remained stable", "sortOrder": 2}, {"optionKey": "C", "content": "dropped dramatically", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'Remained stable means the figure did not change. Fluctuated means it went up and down, and dropped dramatically means a large fall.',
     'Chọn cụm không có sự thay đổi nào.', 'PUBLISHED'),
    ('34000000-0000-4000-8000-070000000009', '34000000-0000-4000-8000-060000000009', 1,
     'Complete with ONE WORD: the number of visitors reached a ______ of 900 in July, its highest point of the year.',
     NULL, '{"type": "FILL", "accepted": ["peak"]}', 1,
     'A peak is the highest point of a trend line, so the noun peak completes the phrase reached a peak of.',
     'Danh từ chỉ điểm cao nhất của đường biểu diễn.', 'PUBLISHED');
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000007' WHERE id = '34000000-0000-4000-8000-060000000007';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000008' WHERE id = '34000000-0000-4000-8000-060000000008';
UPDATE questions SET current_published_version_id = '34000000-0000-4000-8000-070000000009' WHERE id = '34000000-0000-4000-8000-060000000009';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('34000000-0000-4000-8000-070000000007', '34000000-0000-4000-8000-020000000003', 1.0),
    ('34000000-0000-4000-8000-070000000008', '34000000-0000-4000-8000-020000000003', 1.0),
    ('34000000-0000-4000-8000-070000000009', '34000000-0000-4000-8000-020000000003', 1.0);
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('34000000-0000-4000-8000-040000000006', '34000000-0000-4000-8000-070000000007', 1),
    ('34000000-0000-4000-8000-040000000006', '34000000-0000-4000-8000-070000000008', 2),
    ('34000000-0000-4000-8000-040000000006', '34000000-0000-4000-8000-070000000009', 3);

-- Practice material: every knowledge point of the course needs three practice sets of three questions each. The sets
-- are free-standing (no lesson, no topic) and share nothing with the lesson exercises above.
CREATE TEMP TABLE writing_practice_items (
    kp_code TEXT, pkg_code TEXT, pkg_title TEXT, q_no INT, qtype TEXT, stem TEXT,
    opt_a TEXT, opt_b TEXT, opt_c TEXT, correct TEXT, accepted TEXT[], explanation TEXT, hint TEXT, difficulty TEXT
) ON COMMIT DROP;

INSERT INTO writing_practice_items VALUES
-- Topic sentences and introductions
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-1', 'Luyện thêm: Câu chủ đề (học online)', 1, 'MULTIPLE_CHOICE',
 'Which sentence is the best topic sentence for a paragraph about the advantages of learning online?',
 'Online learning lets students study at their own pace and from any location, which makes education more accessible.',
 'I watched a video lesson yesterday.', 'Many websites exist.', 'A', NULL,
 'A states a general main idea with a reason. B is one personal example and C is too vague to organise a paragraph.',
 'Chọn câu nêu ý chính của cả đoạn, không phải một ví dụ.', 'EASY'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-1', 'Luyện thêm: Câu chủ đề (học online)', 2, 'MULTIPLE_CHOICE',
 'Which sentence is too specific to be a topic sentence for a paragraph about the benefits of public services?',
 'Public libraries give residents free access to books and computers.',
 'A library in my town opened in 1998 and has two floors.', 'Low-cost public services help poorer families most.', 'B', NULL,
 'B gives a detail about one building and cannot control a whole paragraph. A and C express ideas that examples can support.',
 'Câu chủ đề phải đủ rộng để bao trùm các ý còn lại của đoạn.', 'MEDIUM'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-1', 'Luyện thêm: Câu chủ đề (học online)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: a topic sentence states the ______ idea of a paragraph.',
 NULL, NULL, NULL, NULL, ARRAY['main', 'central', 'key'],
 'The topic sentence tells the reader the main idea that the rest of the paragraph develops.',
 'Từ cần điền nghĩa là ý quan trọng nhất của đoạn.', 'EASY'),

('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-2', 'Luyện thêm: Diễn đạt lại đề bài', 1, 'MULTIPLE_CHOICE',
 'Which sentence best paraphrases: "Many people believe that city life is better than life in the countryside."?',
 'Many people believe that city life is better than life in the countryside.',
 'A large number of individuals consider urban living preferable to rural living.', 'Cities are big and the countryside is small.', 'B', NULL,
 'B keeps the meaning with different words and structure. A copies the task and C changes the meaning.',
 'Giữ nguyên nghĩa nhưng thay từ ngữ và cấu trúc.', 'MEDIUM'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-2', 'Luyện thêm: Diễn đạt lại đề bài', 2, 'MULTIPLE_CHOICE',
 'Which introduction sentence states the writer''s opinion clearly?',
 'Some people like technology and others do not.',
 'In my opinion, the advantages of technology in education outweigh its drawbacks.', 'This essay is about technology.', 'B', NULL,
 'B takes a clear position on the question. A reports two views without choosing and C only names the topic.',
 'Tìm câu cho biết người viết đứng về phía nào.', 'EASY'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-2', 'Luyện thêm: Diễn đạt lại đề bài', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: in the introduction you should ______ the question in your own words rather than copy it.',
 NULL, NULL, NULL, NULL, ARRAY['paraphrase', 'restate', 'rephrase', 'reword'],
 'Copying the task shows no language ability; the introduction should restate it with different words.',
 'Từ cần điền nghĩa là viết lại bằng từ ngữ khác.', 'MEDIUM'),

('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-3', 'Luyện thêm: Vị trí câu chủ đề', 1, 'MULTIPLE_CHOICE',
 'Where does the topic sentence usually appear in a body paragraph?',
 'At the beginning', 'After the examples', 'In the conclusion only', 'A', NULL,
 'Placing it first tells the reader what the paragraph is about before the support arrives.',
 'Người đọc cần biết ý chính trước khi đọc phần giải thích.', 'EASY'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-3', 'Luyện thêm: Vị trí câu chủ đề', 2, 'MULTIPLE_CHOICE',
 'Which sentence best follows the topic sentence "Regular exercise improves mental health."?',
 'For example, studies show that people who walk daily report lower stress levels.',
 'Exercise is a word with many meanings.', 'In conclusion, I disagree with the statement.', 'A', NULL,
 'A supports the topic sentence with evidence. B changes the subject and C belongs to the conclusion and contradicts the idea.',
 'Câu tiếp theo cần minh họa hoặc chứng minh cho ý chính.', 'MEDIUM'),
('DEMO_WRITING_TOPIC_SENTENCE', 'PS-W3-TS-3', 'Luyện thêm: Vị trí câu chủ đề', 3, 'MULTIPLE_CHOICE',
 'What is wrong with this introduction? "Nowadays technology is very important. Technology is a big topic."',
 'It repeats general statements without paraphrasing the task or giving an opinion.',
 'It is far too long.', 'It uses too many linking words.', 'A', NULL,
 'Both sentences are empty generalisations; a good introduction restates the task and states a position.',
 'Xem phần mở bài có làm được hai việc: diễn đạt lại đề và nêu quan điểm không.', 'MEDIUM'),

-- Linking words and coherence
('DEMO_WRITING_LINKING', 'PS-W4-LK-1', 'Luyện thêm: Từ nối đối lập và bổ sung', 1, 'MULTIPLE_CHOICE',
 'Choose the best linking word: "Cars are convenient; ______, they cause air pollution."',
 'For instance', 'However', 'Because', 'B', NULL,
 'The second clause contrasts with the first, so a contrast word is needed.',
 'Hai vế nói hai điều trái chiều.', 'EASY'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-1', 'Luyện thêm: Từ nối đối lập và bổ sung', 2, 'MULTIPLE_CHOICE',
 'Which word adds a similar idea? "Exercise reduces stress. ______, it improves sleep."',
 'Moreover', 'However', 'Although', 'A', NULL,
 'Both sentences give a benefit of exercise, so an addition word fits. However and although express contrast.',
 'Hai câu cùng nói về lợi ích.', 'EASY'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-1', 'Luyện thêm: Từ nối đối lập và bổ sung', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: ______ some people prefer cities, others enjoy the countryside.',
 NULL, NULL, NULL, NULL, ARRAY['although', 'while', 'whereas'],
 'The sentence contrasts two groups of people, so a contrast conjunction such as although or while is needed.',
 'Từ cần điền mở đầu một mệnh đề đối lập.', 'MEDIUM'),

('DEMO_WRITING_LINKING', 'PS-W4-LK-2', 'Luyện thêm: Nguyên nhân và kết quả', 1, 'MULTIPLE_CHOICE',
 'Which sentence shows a result?',
 'Prices rose; as a result, fewer people bought the product.',
 'Prices rose; for example, bread cost more.', 'Prices rose; however, wages stayed the same.', 'A', NULL,
 'As a result links an effect to its cause. For example introduces an illustration and however a contrast.',
 'Tìm câu nêu hệ quả của việc giá tăng.', 'EASY'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-2', 'Luyện thêm: Nguyên nhân và kết quả', 2, 'MULTIPLE_CHOICE',
 'Which sentence uses "because" correctly?',
 'Because the weather was bad.', 'The match was cancelled because the weather was bad.', 'Because of the weather was bad, the match was cancelled.', 'B', NULL,
 'Because joins two clauses. A is a fragment and C mixes because of with a full clause.',
 'Because nối hai mệnh đề; because of đi với danh từ.', 'MEDIUM'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-2', 'Luyện thêm: Nguyên nhân và kết quả', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD that shows a result: "The roads were icy; ______, many flights were delayed."',
 NULL, NULL, NULL, NULL, ARRAY['consequently', 'therefore', 'thus', 'hence'],
 'Delayed flights are an effect of the icy roads, so a result word is needed.',
 'Chuyến bay trễ là kết quả của đường đóng băng.', 'MEDIUM'),

('DEMO_WRITING_LINKING', 'PS-W4-LK-3', 'Luyện thêm: Ví dụ và kết luận', 1, 'MULTIPLE_CHOICE',
 'Which linking phrase introduces an example?',
 'For instance', 'In contrast', 'As a result', 'A', NULL,
 'For instance and for example introduce an illustration of the previous idea.',
 'Chọn cụm dùng để đưa ví dụ.', 'EASY'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-3', 'Luyện thêm: Ví dụ và kết luận', 2, 'MULTIPLE_CHOICE',
 'Which phrase is the best way to begin the final paragraph of an essay?',
 'For example', 'In conclusion', 'On the other hand', 'B', NULL,
 'In conclusion signals that the essay is finishing and the main points are being summed up.',
 'Đoạn cuối tổng kết lại bài viết.', 'EASY'),
('DEMO_WRITING_LINKING', 'PS-W4-LK-3', 'Luyện thêm: Ví dụ và kết luận', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Some jobs are well paid. ______, they can be very stressful."',
 NULL, NULL, NULL, NULL, ARRAY['however', 'nevertheless', 'nonetheless'],
 'Good pay and stress are in contrast, so a contrast adverb such as however is needed.',
 'Hai ý trái chiều nhau.', 'MEDIUM'),

-- Language of trends
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-1', 'Luyện thêm: Động từ chỉ xu hướng', 1, 'MULTIPLE_CHOICE',
 'Which verb means the same as "increase"?',
 'decline', 'climb', 'fluctuate', 'B', NULL,
 'Climb means to go up. Decline means to go down and fluctuate means to go up and down.',
 'Chọn động từ chỉ sự đi lên.', 'EASY'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-1', 'Luyện thêm: Động từ chỉ xu hướng', 2, 'MULTIPLE_CHOICE',
 'Which phrase describes a small, slow rise?',
 'rose slightly', 'soared', 'collapsed', 'A', NULL,
 'Slightly shows a small change. Soared is a very large rise and collapsed is a very large fall.',
 'Chọn cụm chỉ mức tăng nhỏ.', 'MEDIUM'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-1', 'Luyện thêm: Động từ chỉ xu hướng', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Unemployment ______ gradually from 8% to 5%."',
 NULL, NULL, NULL, NULL, ARRAY['fell', 'declined', 'decreased', 'dropped'],
 'The figure goes down from 8% to 5%, so a past-tense verb of decrease is needed.',
 'Số liệu đi từ 8% xuống 5%, dùng thì quá khứ.', 'MEDIUM'),

('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-2', 'Luyện thêm: Điểm cao nhất và giữ nguyên', 1, 'MULTIPLE_CHOICE',
 'Which noun describes the highest point of a line on a graph?',
 'trough', 'peak', 'level', 'B', NULL,
 'A peak is the highest point. A trough is the lowest point.',
 'Chọn danh từ chỉ điểm cao nhất.', 'EASY'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-2', 'Luyện thêm: Điểm cao nhất và giữ nguyên', 2, 'MULTIPLE_CHOICE',
 'Which expression means "stopped changing and stayed at the same level"?',
 'levelled off', 'plummeted', 'surged', 'A', NULL,
 'Levelled off means the line flattened after changing. Plummeted and surged are sharp falls and rises.',
 'Chọn cụm chỉ đường biểu diễn đi ngang.', 'MEDIUM'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-2', 'Luyện thêm: Điểm cao nhất và giữ nguyên', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Sales ______ stable at 300 units from March to June."',
 NULL, NULL, NULL, NULL, ARRAY['remained', 'stayed'],
 'Remained stable means there was no change during the period.',
 'Từ cần điền đi với stable để chỉ không thay đổi.', 'MEDIUM'),

('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-3', 'Luyện thêm: Mức độ và giới từ', 1, 'MULTIPLE_CHOICE',
 'Which sentence describes a sudden large drop?',
 'Prices rose gradually.', 'Prices fell dramatically.', 'Prices were stable.', 'B', NULL,
 'Fell dramatically shows a large decrease; the other sentences describe a rise and no change.',
 'Chọn câu có giảm mạnh.', 'EASY'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-3', 'Luyện thêm: Mức độ và giới từ', 2, 'MULTIPLE_CHOICE',
 'Choose the correct preposition: "The number rose ______ 40 to 65."',
 'from', 'at', 'by', 'A', NULL,
 'Rose from 40 to 65 gives the starting and the end value. By would show the size of the change.',
 'Giới từ chỉ điểm bắt đầu của sự thay đổi.', 'MEDIUM'),
('DEMO_WRITING_TREND_LANGUAGE', 'PS-W5-TR-3', 'Luyện thêm: Mức độ và giới từ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "The population ______ sharply between 1990 and 2000, from 2 million to 5 million."',
 NULL, NULL, NULL, NULL, ARRAY['grew', 'rose', 'increased', 'climbed'],
 'The population went up, so a past-tense verb of increase fits.',
 'Dân số đi từ 2 triệu lên 5 triệu, dùng thì quá khứ.', 'MEDIUM');

DO $$
DECLARE
    pkg RECORD;
    item RECORD;
    package_id UUID;
    version_id UUID;
    section_id UUID;
    question_id UUID;
    question_version_id UUID;
BEGIN
    FOR pkg IN SELECT DISTINCT pkg_code, pkg_title, kp_code FROM writing_practice_items ORDER BY pkg_code LOOP
        package_id := gen_random_uuid();
        version_id := gen_random_uuid();
        section_id := gen_random_uuid();
        INSERT INTO content_packages (id, code, title, package_type, topic_id, status)
        VALUES (package_id, pkg.pkg_code, pkg.pkg_title, 'PRACTICE_SET', NULL, 'PUBLISHED');
        INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
        VALUES (version_id, package_id, 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);
        UPDATE content_packages SET current_published_version_id = version_id WHERE id = package_id;
        INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
        VALUES (section_id, version_id, pkg.pkg_title, 'WRITING', 1);

        FOR item IN SELECT * FROM writing_practice_items WHERE pkg_code = pkg.pkg_code ORDER BY q_no LOOP
            question_id := gen_random_uuid();
            question_version_id := gen_random_uuid();
            INSERT INTO questions (id, question_type, skill, purpose, status)
            VALUES (question_id, item.qtype, 'WRITING', 'LEARNING', 'PUBLISHED');
            INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version,
                                           explanation, hint, difficulty, status)
            VALUES (question_version_id, question_id, 1, item.stem,
                    CASE WHEN item.qtype = 'MULTIPLE_CHOICE' THEN jsonb_build_array(
                        jsonb_build_object('optionKey', 'A', 'content', item.opt_a, 'sortOrder', 1),
                        jsonb_build_object('optionKey', 'B', 'content', item.opt_b, 'sortOrder', 2),
                        jsonb_build_object('optionKey', 'C', 'content', item.opt_c, 'sortOrder', 3)) END,
                    CASE WHEN item.qtype = 'MULTIPLE_CHOICE'
                         THEN jsonb_build_object('type', 'CHOICE', 'correct', item.correct)
                         ELSE jsonb_build_object('type', 'FILL', 'accepted', to_jsonb(item.accepted)) END,
                    1, item.explanation, item.hint, item.difficulty, 'PUBLISHED');
            UPDATE questions SET current_published_version_id = question_version_id WHERE id = question_id;
            INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
            VALUES (question_version_id, (SELECT id FROM knowledge_points WHERE code = pkg.kp_code), 1.0);
            INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
            VALUES (gen_random_uuid(), section_id, question_version_id, item.q_no, 1.0);
        END LOOP;
    END LOOP;
END $$;
