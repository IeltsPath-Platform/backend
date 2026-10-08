-- Placement test covering Listening, Reading, Writing and Speaking. Every question has purpose PLACEMENT so it is
-- never reused by lessons, practice, topic or course tests. Listening audio is an object key resolved against
-- CONTENT_MEDIA_BASE_URL; its transcript is an answer and only leaves Content through the internal API.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2b000000-0000-4000-8000-050000000001', 'AUDIO', 'Librarian: Good morning, Riverside Library. How can I help you?
Caller: Hi. I would like to book a study room for my group.
Librarian: Of course. Can I have your surname, please?
Caller: Yes, it is Thompson. T, H, O, M, P, S, O, N.
Librarian: Thank you. Which day would you like the room?
Caller: Friday the fifteenth of May, in the afternoon, please.
Librarian: Fine. A room for up to six people costs twelve pounds for three hours.
Caller: Twelve pounds is fine.', 'listening/demo/ls1.mp3', 45, 'VALID');
INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('2b000000-0000-4000-8000-050000000002', 'PASSAGE', 'A. A community centre in a small town started a tool library two years ago. Residents can borrow drills, ladders and garden tools for a week at no charge, provided they show a membership card.

B. The idea came from a survey in which most households said they used a power tool less than twice a year. Buying one seemed wasteful, so the centre collected donations and asked a local shop to repair damaged items.

C. Demand is highest in spring, when many members want garden equipment. Volunteers therefore check every returned tool within a day, so that it can be lent again quickly. Staff say that late returns have been the biggest problem, although fines have not been introduced.', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2b000000-0000-4000-8000-060000000001', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000002', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000004', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000005', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000006', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000007', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000008', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000009', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000010', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000011', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-060000000012', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2b000000-0000-4000-8000-070000000001', '2b000000-0000-4000-8000-060000000001', 1, 'Day of the booking: ______', NULL,
     '{"type": "FILL", "accepted": ["friday"]}'::jsonb, 1, 'The caller asks for Friday the fifteenth of May.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000002', '2b000000-0000-4000-8000-060000000002', 1, 'How many people can the room hold at most?',
     '[{"optionKey": "A", "content": "Four", "sortOrder": 1}, {"optionKey": "B", "content": "Six", "sortOrder": 2}, {"optionKey": "C", "content": "Twelve", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'A room for up to six people.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000003', '2b000000-0000-4000-8000-060000000003', 1, 'Cost of the room for three hours (£): ______', NULL,
     '{"type": "FILL", "accepted": ["12", "twelve"]}'::jsonb, 1, 'Twelve pounds for three hours.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000004', '2b000000-0000-4000-8000-060000000004', 1, 'What does the caller spell out?',
     '[{"optionKey": "A", "content": "The name of the library", "sortOrder": 1}, {"optionKey": "B", "content": "Her surname", "sortOrder": 2}, {"optionKey": "C", "content": "Her address", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'She spells T, H, O, M, P, S, O, N.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000005', '2b000000-0000-4000-8000-060000000005', 1, 'How long can a member keep a tool?',
     '[{"optionKey": "A", "content": "One day", "sortOrder": 1}, {"optionKey": "B", "content": "One week", "sortOrder": 2}, {"optionKey": "C", "content": "Two years", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Paragraph A: tools are borrowed for a week.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000006', '2b000000-0000-4000-8000-060000000006', 1, 'Why did the centre set up the tool library?',
     '[{"optionKey": "A", "content": "Households rarely used tools they might buy", "sortOrder": 1}, {"optionKey": "B", "content": "Local shops refused to repair tools", "sortOrder": 2}, {"optionKey": "C", "content": "Demand for garden equipment was low", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Paragraph B: most households used a power tool less than twice a year.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000007', '2b000000-0000-4000-8000-060000000007', 1, 'Members must pay a fine for returning tools late.',
     '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Paragraph C: fines have not been introduced.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000008', '2b000000-0000-4000-8000-060000000008', 1, 'The local shop donated the first tools.',
     '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'The shop repaired items; who donated the first tools is not stated.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000009', '2b000000-0000-4000-8000-060000000009', 1, 'Volunteers check each returned tool within ______.', NULL,
     '{"type": "FILL", "accepted": ["a day", "one day", "1 day"]}'::jsonb, 1, 'Paragraph C: within a day.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000010', '2b000000-0000-4000-8000-060000000010', 1, 'What does the passage say about spring?',
     '[{"optionKey": "A", "content": "Fewer people join the centre", "sortOrder": 1}, {"optionKey": "B", "content": "Demand for garden equipment is highest", "sortOrder": 2}, {"optionKey": "C", "content": "The tool library closes", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Paragraph C: demand is highest in spring.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000011', '2b000000-0000-4000-8000-060000000011', 1, 'Some people think that cities should plant trees along every street, even if this means fewer parking spaces. To what extent do you agree or disagree? Give reasons for your answer. Write at least 150 words.', NULL,
     '{"type": "ESSAY", "task": "TASK_2", "minWords": 150, "passBand": 5.5}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('2b000000-0000-4000-8000-070000000012', '2b000000-0000-4000-8000-060000000012', 1, 'Describe a place you like to visit in your free time. You should say where it is, how often you go there, what you do there, and explain why you like it. Speak for one to two minutes.', NULL,
     '{"type": "SPEAKING", "durationSeconds": 120}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED');

UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000001' WHERE id = '2b000000-0000-4000-8000-060000000001';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000002' WHERE id = '2b000000-0000-4000-8000-060000000002';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000003' WHERE id = '2b000000-0000-4000-8000-060000000003';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000004' WHERE id = '2b000000-0000-4000-8000-060000000004';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000005' WHERE id = '2b000000-0000-4000-8000-060000000005';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000006' WHERE id = '2b000000-0000-4000-8000-060000000006';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000007' WHERE id = '2b000000-0000-4000-8000-060000000007';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000008' WHERE id = '2b000000-0000-4000-8000-060000000008';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000009' WHERE id = '2b000000-0000-4000-8000-060000000009';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000010' WHERE id = '2b000000-0000-4000-8000-060000000010';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000011' WHERE id = '2b000000-0000-4000-8000-060000000011';
UPDATE questions SET current_published_version_id = '2b000000-0000-4000-8000-070000000012' WHERE id = '2b000000-0000-4000-8000-060000000012';

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key)
VALUES ('2b000000-0000-4000-8000-080000000001', 'PLACEMENT-4SKILLS', 'IELTS placement test', 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL);
INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version)
VALUES ('2b000000-0000-4000-8000-090000000001', '2b000000-0000-4000-8000-080000000001', 1, 'PUBLISHED', '{}', 1);
UPDATE content_packages SET current_published_version_id = '2b000000-0000-4000-8000-090000000001'
WHERE id = '2b000000-0000-4000-8000-080000000001';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) VALUES
    ('2b000000-0000-4000-8000-0a0000000001', '2b000000-0000-4000-8000-090000000001', 'Listening: booking a study room', 'LISTENING', 1),
    ('2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-090000000001', 'Reading: a community tool library', 'READING', 2),
    ('2b000000-0000-4000-8000-0a0000000003', '2b000000-0000-4000-8000-090000000001', 'Writing: trees or parking', 'WRITING', 3),
    ('2b000000-0000-4000-8000-0a0000000004', '2b000000-0000-4000-8000-090000000001', 'Speaking: a place you like', 'SPEAKING', 4);

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2b000000-0000-4000-8000-0c0000000001', '2b000000-0000-4000-8000-050000000001', '2b000000-0000-4000-8000-0a0000000001', 0),
    ('2b000000-0000-4000-8000-0c0000000002', '2b000000-0000-4000-8000-050000000002', '2b000000-0000-4000-8000-0a0000000002', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('2b000000-0000-4000-8000-0b0000000001', '2b000000-0000-4000-8000-0a0000000001', '2b000000-0000-4000-8000-070000000001', 1, 1.0),
    ('2b000000-0000-4000-8000-0b0000000002', '2b000000-0000-4000-8000-0a0000000001', '2b000000-0000-4000-8000-070000000002', 2, 1.0),
    ('2b000000-0000-4000-8000-0b0000000003', '2b000000-0000-4000-8000-0a0000000001', '2b000000-0000-4000-8000-070000000003', 3, 1.0),
    ('2b000000-0000-4000-8000-0b0000000004', '2b000000-0000-4000-8000-0a0000000001', '2b000000-0000-4000-8000-070000000004', 4, 1.0),
    ('2b000000-0000-4000-8000-0b0000000005', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000005', 1, 1.0),
    ('2b000000-0000-4000-8000-0b0000000006', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000006', 2, 1.0),
    ('2b000000-0000-4000-8000-0b0000000007', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000007', 3, 1.0),
    ('2b000000-0000-4000-8000-0b0000000008', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000008', 4, 1.0),
    ('2b000000-0000-4000-8000-0b0000000009', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000009', 5, 1.0),
    ('2b000000-0000-4000-8000-0b0000000010', '2b000000-0000-4000-8000-0a0000000002', '2b000000-0000-4000-8000-070000000010', 6, 1.0),
    ('2b000000-0000-4000-8000-0b0000000011', '2b000000-0000-4000-8000-0a0000000003', '2b000000-0000-4000-8000-070000000011', 1, 1.0),
    ('2b000000-0000-4000-8000-0b0000000012', '2b000000-0000-4000-8000-0a0000000004', '2b000000-0000-4000-8000-070000000012', 1, 1.0);
