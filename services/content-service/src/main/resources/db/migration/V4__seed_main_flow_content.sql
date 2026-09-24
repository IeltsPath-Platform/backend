-- Small published content package for exercising the assessment flow locally.
-- Stable UUIDs make the package, section, question, and knowledge point easy to
-- reference while debugging across Content and Assessment Service.

INSERT INTO topics (id, code, name, sort_order, status)
VALUES (
    '10000000-0000-4000-8000-000000000001',
    'DEMO_READING',
    'Demo IELTS Reading',
    900,
    'ACTIVE'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status)
SELECT
    '10000000-0000-4000-8000-000000000002',
    t.id,
    'DEMO_READING_MAIN_IDEA',
    'Identify the main idea',
    'STRATEGY',
    'PROCEDURE',
    'READING',
    'Choose the option that best summarizes the passage.',
    'ACTIVE'
FROM topics t
WHERE t.code = 'DEMO_READING'
ON CONFLICT (code) DO NOTHING;

INSERT INTO content_packages (id, code, title, package_type, required_feature_key, status)
VALUES (
    '10000000-0000-4000-8000-000000000003',
    'DEMO_MAIN_FLOW_READING',
    'Demo main flow - IELTS Reading',
    'PRACTICE_SET',
    NULL,
    'DRAFT'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
SELECT
    '10000000-0000-4000-8000-000000000004',
    p.id,
    1,
    'PUBLISHED',
    '{"demo": true}'::jsonb,
    1,
    CURRENT_TIMESTAMP
FROM content_packages p
WHERE p.code = 'DEMO_MAIN_FLOW_READING'
ON CONFLICT (package_id, version_number) DO NOTHING;

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions)
SELECT
    '10000000-0000-4000-8000-000000000005',
    pv.id,
    'Reading: main idea',
    'READING',
    1,
    300,
    'Read the passage and choose its main idea.'
FROM content_package_versions pv
JOIN content_packages p ON p.id = pv.package_id
WHERE p.code = 'DEMO_MAIN_FLOW_READING'
  AND pv.version_number = 1
ON CONFLICT (package_version_id, sort_order) DO NOTHING;

INSERT INTO questions (id, question_type, skill, required_feature_key, status)
VALUES (
    '10000000-0000-4000-8000-000000000006',
    'MULTIPLE_CHOICE',
    'READING',
    NULL,
    'DRAFT'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, difficulty, status)
SELECT
    '10000000-0000-4000-8000-000000000007',
    q.id,
    1,
    'What is the passage mainly about?',
    '[{"optionKey":"A","content":"The benefits of urban trees","sortOrder":1},{"optionKey":"B","content":"How to build a city railway","sortOrder":2},{"optionKey":"C","content":"The history of public libraries","sortOrder":3}]'::jsonb,
    '{"correct":"A"}'::jsonb,
    1,
    'The passage focuses on how urban trees improve city life.',
    'EASY',
    'PUBLISHED'
FROM questions q
WHERE q.id = '10000000-0000-4000-8000-000000000006'
ON CONFLICT (question_id, version_number) DO NOTHING;

UPDATE questions
SET status = 'PUBLISHED',
    current_published_version_id = '10000000-0000-4000-8000-000000000007',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '10000000-0000-4000-8000-000000000006';

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT
    qv.id,
    kp.id,
    1.00
FROM question_versions qv
JOIN questions q ON q.id = qv.question_id
JOIN knowledge_points kp ON kp.code = 'DEMO_READING_MAIN_IDEA'
WHERE q.id = '10000000-0000-4000-8000-000000000006'
  AND qv.version_number = 1
ON CONFLICT (question_version_id, knowledge_point_id) DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
SELECT
    '10000000-0000-4000-8000-000000000008',
    s.id,
    qv.id,
    1,
    1.00
FROM content_sections s
JOIN content_package_versions pv ON pv.id = s.package_version_id
JOIN content_packages p ON p.id = pv.package_id
JOIN questions q ON q.id = '10000000-0000-4000-8000-000000000006'
JOIN question_versions qv ON qv.question_id = q.id AND qv.version_number = 1
WHERE p.code = 'DEMO_MAIN_FLOW_READING'
  AND s.sort_order = 1
ON CONFLICT (section_id, sort_order) DO NOTHING;

UPDATE content_packages
SET status = 'PUBLISHED',
    current_published_version_id = '10000000-0000-4000-8000-000000000004',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '10000000-0000-4000-8000-000000000003';
