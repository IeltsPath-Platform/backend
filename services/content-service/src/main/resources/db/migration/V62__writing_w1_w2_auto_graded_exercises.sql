-- Lessons W1 and W2 only hold an essay exercise, which the learner does not see among the auto-graded exercises. Once
-- they got practice sets, a learner could not finish either lesson: Practice opens only after the lesson is completed,
-- and a lesson with practice but no exercises asks for the Practice first. Each of the two lessons now gets an
-- auto-graded exercise block of three questions, like W3 to W5, so the learner completes the lesson by passing it.

INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES
    ('35000000-0000-4000-8000-040000000001', '25000000-0000-4000-8000-030000000001', 3, 'EXERCISE'),
    ('35000000-0000-4000-8000-040000000002', '25000000-0000-4000-8000-030000000002', 3, 'EXERCISE');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('35000000-0000-4000-8000-060000000001', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('35000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('35000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('35000000-0000-4000-8000-060000000004', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('35000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'WRITING', 'LEARNING', 'PUBLISHED'),
    ('35000000-0000-4000-8000-060000000006', 'FILL_IN_BLANK', 'WRITING', 'LEARNING', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, hint, difficulty, status) VALUES
    -- W1: describing a chart (Task 1)
    ('35000000-0000-4000-8000-070000000001', '35000000-0000-4000-8000-060000000001', 1,
     'What should the introduction of a Task 1 report do?',
     '[{"optionKey": "A", "content": "Give an opinion about the data.", "sortOrder": 1}, {"optionKey": "B", "content": "Paraphrase the description of the chart.", "sortOrder": 2}, {"optionKey": "C", "content": "List all the figures.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'The introduction restates what the chart shows in your own words. Opinions are not wanted in Task 1 and figures belong to the body paragraphs.',
     'Mở bài chỉ diễn đạt lại đề, chưa đi vào số liệu.', 'EASY', 'PUBLISHED'),
    ('35000000-0000-4000-8000-070000000002', '35000000-0000-4000-8000-060000000002', 1,
     'Which feature of a chart should the overview mention?',
     '[{"optionKey": "A", "content": "The single smallest figure.", "sortOrder": 1}, {"optionKey": "B", "content": "The most noticeable trend or difference.", "sortOrder": 2}, {"optionKey": "C", "content": "The writer''s personal experience.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'The overview summarises the main trend or the biggest difference; it is not a place for minor details or personal experience.',
     'Overview nêu điểm nổi bật nhất của biểu đồ.', 'EASY', 'PUBLISHED'),
    ('35000000-0000-4000-8000-070000000003', '35000000-0000-4000-8000-060000000003', 1,
     'Complete with ONE WORD: in academic Writing Task 1 you must write at least 150 ______.',
     NULL, '{"type": "FILL", "accepted": ["words"]}', 1,
     'The minimum length of a Task 1 report is 150 words.',
     'Số lượng tối thiểu của bài viết Task 1.', 'EASY', 'PUBLISHED'),
    -- W2: opinion essay (Task 2)
    ('35000000-0000-4000-8000-070000000004', '35000000-0000-4000-8000-060000000004', 1,
     'Which kind of essay is the question "To what extent do you agree or disagree?"',
     '[{"optionKey": "A", "content": "An opinion essay.", "sortOrder": 1}, {"optionKey": "B", "content": "A description of a chart.", "sortOrder": 2}, {"optionKey": "C", "content": "A formal letter.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "A"}', 1,
     'The question asks for the writer''s own position, so it is an opinion essay.',
     'Câu hỏi yêu cầu bạn nêu quan điểm của mình.', 'EASY', 'PUBLISHED'),
    ('35000000-0000-4000-8000-070000000005', '35000000-0000-4000-8000-060000000005', 1,
     'Which order of paragraphs is best for an opinion essay?',
     '[{"optionKey": "A", "content": "Conclusion, body paragraphs, introduction.", "sortOrder": 1}, {"optionKey": "B", "content": "Introduction, body paragraphs, conclusion.", "sortOrder": 2}, {"optionKey": "C", "content": "Body paragraphs, introduction, conclusion.", "sortOrder": 3}]',
     '{"type": "CHOICE", "correct": "B"}', 1,
     'An essay opens with the position, develops it in the body paragraphs and ends with a conclusion.',
     'Bài luận đi từ mở bài đến kết luận.', 'EASY', 'PUBLISHED'),
    ('35000000-0000-4000-8000-070000000006', '35000000-0000-4000-8000-060000000006', 1,
     'Complete with ONE WORD: in Writing Task 2 you should write at least 250 ______.',
     NULL, '{"type": "FILL", "accepted": ["words"]}', 1,
     'The minimum length of a Task 2 essay is 250 words.',
     'Số lượng tối thiểu của bài viết Task 2.', 'EASY', 'PUBLISHED');

UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000001' WHERE id = '35000000-0000-4000-8000-060000000001';
UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000002' WHERE id = '35000000-0000-4000-8000-060000000002';
UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000003' WHERE id = '35000000-0000-4000-8000-060000000003';
UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000004' WHERE id = '35000000-0000-4000-8000-060000000004';
UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000005' WHERE id = '35000000-0000-4000-8000-060000000005';
UPDATE questions SET current_published_version_id = '35000000-0000-4000-8000-070000000006' WHERE id = '35000000-0000-4000-8000-060000000006';

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('35000000-0000-4000-8000-070000000001', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W1_CHART'), 1.0),
    ('35000000-0000-4000-8000-070000000002', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W1_CHART'), 1.0),
    ('35000000-0000-4000-8000-070000000003', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W1_CHART'), 1.0),
    ('35000000-0000-4000-8000-070000000004', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W2_OPINION'), 1.0),
    ('35000000-0000-4000-8000-070000000005', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W2_OPINION'), 1.0),
    ('35000000-0000-4000-8000-070000000006', (SELECT id FROM knowledge_points WHERE code = 'DEMO_READING_W2_OPINION'), 1.0);

INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('35000000-0000-4000-8000-040000000001', '35000000-0000-4000-8000-070000000001', 1),
    ('35000000-0000-4000-8000-040000000001', '35000000-0000-4000-8000-070000000002', 2),
    ('35000000-0000-4000-8000-040000000001', '35000000-0000-4000-8000-070000000003', 3),
    ('35000000-0000-4000-8000-040000000002', '35000000-0000-4000-8000-070000000004', 1),
    ('35000000-0000-4000-8000-040000000002', '35000000-0000-4000-8000-070000000005', 2),
    ('35000000-0000-4000-8000-040000000002', '35000000-0000-4000-8000-070000000006', 3);
