-- Placement gets a Writing Task 1 (a table to describe) beside the existing Task 2 essay, and its sections follow the
-- order of the real computer-based test: Reading, Listening, Writing Task 1, Writing Task 2, Speaking.
-- The table is a PASSAGE asset in "label | value | ..." rows; the grader reads the same figures from chartFacts.
-- Attempts started earlier keep the structure they snapshotted.

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('2b000000-0000-4000-8000-050000000003', 'PASSAGE', 'Museum | 2019 | 2020 | 2021 | 2022 | 2023
Science Museum | 420 | 150 | 210 | 380 | 460
Art Gallery | 310 | 90 | 140 | 260 | 300
History Museum | 180 | 60 | 95 | 170 | 230', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2b000000-0000-4000-8000-060000000013', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2b000000-0000-4000-8000-070000000013', '2b000000-0000-4000-8000-060000000013', 1,
     'The table below shows the number of visitors (in thousands) to three museums in one city from 2019 to 2023. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.',
     NULL,
     '{"type": "ESSAY", "task": "TASK_1", "minWords": 150, "passBand": 5.5, "chartFacts": "Visitors to three museums in one city, in thousands, 2019 to 2023. Science Museum: 420, 150, 210, 380, 460. Art Gallery: 310, 90, 140, 260, 300. History Museum: 180, 60, 95, 170, 230. Key features: all three fell sharply in 2020 (Science Museum -64%, Art Gallery -71%, History Museum -67%) and recovered steadily from 2021; the Science Museum had the most visitors every year; by 2023 the Science Museum (460) and History Museum (230) exceeded their 2019 levels while the Art Gallery (300) stayed slightly below its 2019 figure of 310."}'::jsonb,
     1, 'Graded by the placement essay grader against chartFacts.', 'PUBLISHED');

UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000013'
WHERE id = '2b000000-0000-4000-8000-060000000013';

-- Move every section out of the way first: (package_version_id, sort_order) is unique.
UPDATE content_sections SET sort_order = sort_order + 100
WHERE package_version_id = '2b000000-0000-4000-8000-090000000001';
UPDATE content_sections SET sort_order = 1 WHERE id = '2b000000-0000-4000-8000-0a0000000002';
UPDATE content_sections SET sort_order = 2 WHERE id = '2b000000-0000-4000-8000-0a0000000001';
UPDATE content_sections SET sort_order = 4, title = 'Writing Task 2: trees or parking'
WHERE id = '2b000000-0000-4000-8000-0a0000000003';
UPDATE content_sections SET sort_order = 5 WHERE id = '2b000000-0000-4000-8000-0a0000000004';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES
    ('2b000000-0000-4000-8000-0a0000000005', '2b000000-0000-4000-8000-090000000001', 'Writing Task 1: museum visitors', 'WRITING', 3);

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2b000000-0000-4000-8000-0c0000000003', '2b000000-0000-4000-8000-050000000003', '2b000000-0000-4000-8000-0a0000000005', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('2b000000-0000-4000-8000-0b0000000013', '2b000000-0000-4000-8000-0a0000000005', '2b000000-0000-4000-8000-070000000013', 1, 1.0);
