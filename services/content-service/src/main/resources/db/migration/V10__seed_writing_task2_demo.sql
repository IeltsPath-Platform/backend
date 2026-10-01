-- Writing Task 2 demo: an opinion-essay knowledge point (KP7), one essay question and an essay block at the end of
-- L4 (the last DEMO_READING lesson). The essay is graded by Learning Service, so its answer spec is ungradable for
-- automatic graders; the explanation holds the model answer shown once the learner passes.

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('21000000-0000-4000-8000-020000000007', '10000000-0000-4000-8000-000000000001', 'DEMO_READING_W2_OPINION',
     'Luận quan điểm (Task 2)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Viết bài luận nêu quan điểm: mỗi đoạn thân bài mở bằng một câu chủ đề rồi chứng minh bằng lý do và ví dụ.',
     'ACTIVE');

INSERT INTO questions (id, question_type, skill, status) VALUES
    ('21000000-0000-4000-8000-060000000001', 'ESSAY', 'WRITING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('21000000-0000-4000-8000-070000000001', '21000000-0000-4000-8000-060000000001', 1,
     'Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.',
     NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":6}', 1,
     'Many cities now face hotter summers and more frequent flooding, so it is tempting to make green roofs compulsory on all new buildings. While I accept that such roofs have real advantages, I disagree that the rule should apply to every building.

The case for green roofs is strong. Plants and soil keep the rooms below cooler, which lowers demand for air conditioning during heatwaves. The soil also holds rainwater and releases it gradually, easing pressure on drains. Where these roofs are widespread, as in parts of Copenhagen, flooding after heavy storms has become less of a problem.

However, a single rule for all buildings ignores important differences between them. Installing a green roof adds considerably to construction costs, and the plants need watering, weeding and inspection for decades. A developer building a large office block can spread these costs across many tenants, but the owner of a small house cannot. In addition, some roofs are too steep or too light to carry wet soil safely, and forcing a green roof onto them would create structural risks rather than solve environmental ones.

A better approach would be to target the requirement. Large residential and commercial buildings, where the benefits are greatest, could be obliged to include green roofs, while smaller projects could receive tax reductions or grants if their owners choose to install one.

In conclusion, green roofs deserve support, but making them compulsory for every new building would be unfair and, in some cases, unsafe. A targeted rule combined with incentives would achieve most of the benefits at a fraction of the cost.',
     'PUBLISHED');
UPDATE questions SET current_published_version_id = '21000000-0000-4000-8000-070000000001'
WHERE id = '21000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('21000000-0000-4000-8000-070000000001', '21000000-0000-4000-8000-020000000007', 1.00);

-- L4: a TEXT block introducing the essay, then the essay block.
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES
    ('20000000-0000-4000-8000-030000000004', '21000000-0000-4000-8000-020000000007');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES
    ('21000000-0000-4000-8000-040000000001', '20000000-0000-4000-8000-030000000004', 4, 'TEXT',
     'Dùng chính bài đọc về green roofs để viết một bài luận nêu quan điểm. Mỗi đoạn thân bài mở bằng một câu chủ đề, như bạn vừa luyện khi đọc.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES
    ('21000000-0000-4000-8000-040000000002', '20000000-0000-4000-8000-030000000004', 5, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('21000000-0000-4000-8000-040000000002', '21000000-0000-4000-8000-070000000001', 1);
