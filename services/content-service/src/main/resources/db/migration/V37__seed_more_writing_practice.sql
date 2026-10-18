-- Writing practice test library, second batch: six more tests (PT-WRITING-07 to PT-WRITING-12), each with a Task 1
-- and a Task 2 section. Prompts are original text written for this project; Task 1 figures are given in the prompt
-- because no chart images ship with the migration. Cover images are small inline SVG data URIs. Every statement is
-- idempotent.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000002001', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZTc0OTAiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002002', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZTc0OTAiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002003', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM5MzMzZWEiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002004', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM5MzMzZWEiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002005', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiNDUzMDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002006', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiNDUzMDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002007', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002008', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002009', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiZTEyM2MiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002010', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiZTEyM2MiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002011', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM0MzM4Y2EiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000002012', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM0MzM4Y2EiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID')
ON CONFLICT DO NOTHING;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000002001', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002002', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002003', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002004', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002005', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002006', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002007', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002008', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002009', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002010', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002011', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000002012', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000002001', '2d000000-0000-4000-8000-030000002001', 1, 'The table below shows the average number of visits each person made to public libraries in one year, by age group, in 2000 and in 2020. Under 16: 9 visits in 2000 and 6 in 2020; 16 to 24: 4 and 3; 25 to 44: 5 and 4; 45 to 64: 7 and 5; 65 and over: 11 and 12. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002002', '2d000000-0000-4000-8000-030000002002', 1, 'Some people think that schools should spend more time teaching practical skills, such as cooking and managing money, and less time on academic subjects. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002003', '2d000000-0000-4000-8000-030000002003', 1, 'The line graph below shows the average rainfall, in millimetres, in Lisbon and Dublin in each month of the year. Dublin stays between 50 and 85 mm all year, with a low of 50 mm in April and a high of 85 mm in November. Lisbon starts the year at 100 mm in January, falls to 5 mm in July, then climbs to 110 mm in December. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002004', '2d000000-0000-4000-8000-030000002004', 1, 'Some people believe that governments should fund museums and theatres, while others think this money should be spent on healthcare and education. Discuss both views and give your own opinion. Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002005', '2d000000-0000-4000-8000-030000002005', 1, 'The diagram below shows the stages in making recycled paper. Stage 1: waste paper is collected and sorted. Stage 2: it is mixed with water in a large tank and broken down into pulp. Stage 3: the pulp passes through screens that remove staples and plastic. Stage 4: ink is washed out with chemicals. Stage 5: the clean pulp is pressed between rollers to form wet sheets. Stage 6: the sheets are dried by heated cylinders. Stage 7: the paper is rolled up and cut to size. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002006', '2d000000-0000-4000-8000-030000002006', 1, 'More and more students now take university courses entirely online instead of attending classes on campus. Do the advantages of this development outweigh the disadvantages? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002007', '2d000000-0000-4000-8000-030000002007', 1, 'The two maps below show a seaside town in 1990 and today. In 1990 the town had a fishing harbour in the east, a fish market on the harbour road, a car park beside the beach in the south, a small hotel in the north and farmland to the west. Today the harbour is a marina with cafes, the fish market has closed, the car park has been replaced by a promenade and a beach cafe, the hotel has grown into a large resort, the farmland has become a housing estate, and a new bypass runs around the north of the town. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002008', '2d000000-0000-4000-8000-030000002008', 1, 'In many countries the number of overweight children is rising. What are the causes of this, and what can be done to solve the problem? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002009', '2d000000-0000-4000-8000-030000002009', 1, 'The bar chart below shows the percentage of national electricity produced from renewable sources in four countries in 2000 and in 2020. Denmark: 17% in 2000 and 62% in 2020; Spain: 20% and 44%; Canada: 60% and 68%; Australia: 8% and 29%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002010', '2d000000-0000-4000-8000-030000002010', 1, 'Many people now work long hours and have little free time. What are the reasons for this, and is it a positive or a negative development? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002011', '2d000000-0000-4000-8000-030000002011', 1, 'The pie charts below show how the household waste of one city was handled in 2000 and in 2020. In 2000: landfill 70%, recycling 15%, incineration 10%, composting 5%. In 2020: landfill 28%, recycling 42%, incineration 12%, composting 18%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000002012', '2d000000-0000-4000-8000-030000002012', 1, 'Some people believe that robots and automation will take over most jobs in the future, leaving many people without work. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED')
ON CONFLICT DO NOTHING;

UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002001' WHERE id = '2d000000-0000-4000-8000-030000002001';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002002' WHERE id = '2d000000-0000-4000-8000-030000002002';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002003' WHERE id = '2d000000-0000-4000-8000-030000002003';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002004' WHERE id = '2d000000-0000-4000-8000-030000002004';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002005' WHERE id = '2d000000-0000-4000-8000-030000002005';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002006' WHERE id = '2d000000-0000-4000-8000-030000002006';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002007' WHERE id = '2d000000-0000-4000-8000-030000002007';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002008' WHERE id = '2d000000-0000-4000-8000-030000002008';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002009' WHERE id = '2d000000-0000-4000-8000-030000002009';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002010' WHERE id = '2d000000-0000-4000-8000-030000002010';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002011' WHERE id = '2d000000-0000-4000-8000-030000002011';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000002012' WHERE id = '2d000000-0000-4000-8000-030000002012';

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000002001', 'PT-WRITING-07', 'Writing Practice Test 7', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000002002', 'PT-WRITING-08', 'Writing Practice Test 8', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000002003', 'PT-WRITING-09', 'Writing Practice Test 9', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000002004', 'PT-WRITING-10', 'Writing Practice Test 10', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000002005', 'PT-WRITING-11', 'Writing Practice Test 11', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000002006', 'PT-WRITING-12', 'Writing Practice Test 12', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS')
ON CONFLICT DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000002001', '2d000000-0000-4000-8000-050000002001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000002002', '2d000000-0000-4000-8000-050000002002', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000002003', '2d000000-0000-4000-8000-050000002003', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000002004', '2d000000-0000-4000-8000-050000002004', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000002005', '2d000000-0000-4000-8000-050000002005', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000002006', '2d000000-0000-4000-8000-050000002006', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002001' WHERE id = '2d000000-0000-4000-8000-050000002001';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002002' WHERE id = '2d000000-0000-4000-8000-050000002002';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002003' WHERE id = '2d000000-0000-4000-8000-050000002003';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002004' WHERE id = '2d000000-0000-4000-8000-050000002004';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002005' WHERE id = '2d000000-0000-4000-8000-050000002005';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000002006' WHERE id = '2d000000-0000-4000-8000-050000002006';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no, cover_asset_id) VALUES
    ('2d000000-0000-4000-8000-070000002001', '2d000000-0000-4000-8000-060000002001', 'Task 1: Library visits by age group', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002001'),
    ('2d000000-0000-4000-8000-070000002002', '2d000000-0000-4000-8000-060000002001', 'Task 2: Practical skills at school', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002002'),
    ('2d000000-0000-4000-8000-070000002003', '2d000000-0000-4000-8000-060000002002', 'Task 1: Monthly rainfall in two cities', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002003'),
    ('2d000000-0000-4000-8000-070000002004', '2d000000-0000-4000-8000-060000002002', 'Task 2: Public money for the arts', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002004'),
    ('2d000000-0000-4000-8000-070000002005', '2d000000-0000-4000-8000-060000002003', 'Task 1: How recycled paper is made', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002005'),
    ('2d000000-0000-4000-8000-070000002006', '2d000000-0000-4000-8000-060000002003', 'Task 2: Studying entirely online', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002006'),
    ('2d000000-0000-4000-8000-070000002007', '2d000000-0000-4000-8000-060000002004', 'Task 1: A seaside town then and now', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002007'),
    ('2d000000-0000-4000-8000-070000002008', '2d000000-0000-4000-8000-060000002004', 'Task 2: Overweight children', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002008'),
    ('2d000000-0000-4000-8000-070000002009', '2d000000-0000-4000-8000-060000002005', 'Task 1: Electricity from renewable sources', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002009'),
    ('2d000000-0000-4000-8000-070000002010', '2d000000-0000-4000-8000-060000002005', 'Task 2: Long working hours', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002010'),
    ('2d000000-0000-4000-8000-070000002011', '2d000000-0000-4000-8000-060000002006', 'Task 1: How a city handled its waste', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000002011'),
    ('2d000000-0000-4000-8000-070000002012', '2d000000-0000-4000-8000-060000002006', 'Task 2: Automation and jobs', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000002012')
ON CONFLICT DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000002001', '2d000000-0000-4000-8000-070000002001', 1, 'TABLE_CHART', 'Questions 1-1. table chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002002', '2d000000-0000-4000-8000-070000002002', 1, 'OPINION_ESSAY', 'Questions 1-1. opinion essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000002003', '2d000000-0000-4000-8000-070000002003', 1, 'LINE_GRAPH', 'Questions 1-1. line graph task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002004', '2d000000-0000-4000-8000-070000002004', 1, 'DISCUSSION_ESSAY', 'Questions 1-1. discussion essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000002005', '2d000000-0000-4000-8000-070000002005', 1, 'PROCESS_DIAGRAM', 'Questions 1-1. process diagram task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002006', '2d000000-0000-4000-8000-070000002006', 1, 'ADVANTAGES_DISADVANTAGES_ESSAY', 'Questions 1-1. advantages disadvantages essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000002007', '2d000000-0000-4000-8000-070000002007', 1, 'MAP_DIAGRAM', 'Questions 1-1. map diagram task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002008', '2d000000-0000-4000-8000-070000002008', 1, 'PROBLEM_SOLUTION_ESSAY', 'Questions 1-1. problem solution essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000002009', '2d000000-0000-4000-8000-070000002009', 1, 'BAR_CHART', 'Questions 1-1. bar chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002010', '2d000000-0000-4000-8000-070000002010', 1, 'TWO_PART_QUESTION', 'Questions 1-1. two part question task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000002011', '2d000000-0000-4000-8000-070000002011', 1, 'PIE_CHART', 'Questions 1-1. pie chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000002012', '2d000000-0000-4000-8000-070000002012', 1, 'OPINION_ESSAY', 'Questions 1-1. opinion essay task. Write at least 250 words.')
ON CONFLICT DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000002001', '2d000000-0000-4000-8000-070000002001', '2d000000-0000-4000-8000-040000002001', 1, 1.0, '2d000000-0000-4000-8000-090000002001'),
    ('2d000000-0000-4000-8000-080000002002', '2d000000-0000-4000-8000-070000002002', '2d000000-0000-4000-8000-040000002002', 1, 1.0, '2d000000-0000-4000-8000-090000002002'),
    ('2d000000-0000-4000-8000-080000002003', '2d000000-0000-4000-8000-070000002003', '2d000000-0000-4000-8000-040000002003', 1, 1.0, '2d000000-0000-4000-8000-090000002003'),
    ('2d000000-0000-4000-8000-080000002004', '2d000000-0000-4000-8000-070000002004', '2d000000-0000-4000-8000-040000002004', 1, 1.0, '2d000000-0000-4000-8000-090000002004'),
    ('2d000000-0000-4000-8000-080000002005', '2d000000-0000-4000-8000-070000002005', '2d000000-0000-4000-8000-040000002005', 1, 1.0, '2d000000-0000-4000-8000-090000002005'),
    ('2d000000-0000-4000-8000-080000002006', '2d000000-0000-4000-8000-070000002006', '2d000000-0000-4000-8000-040000002006', 1, 1.0, '2d000000-0000-4000-8000-090000002006'),
    ('2d000000-0000-4000-8000-080000002007', '2d000000-0000-4000-8000-070000002007', '2d000000-0000-4000-8000-040000002007', 1, 1.0, '2d000000-0000-4000-8000-090000002007'),
    ('2d000000-0000-4000-8000-080000002008', '2d000000-0000-4000-8000-070000002008', '2d000000-0000-4000-8000-040000002008', 1, 1.0, '2d000000-0000-4000-8000-090000002008'),
    ('2d000000-0000-4000-8000-080000002009', '2d000000-0000-4000-8000-070000002009', '2d000000-0000-4000-8000-040000002009', 1, 1.0, '2d000000-0000-4000-8000-090000002009'),
    ('2d000000-0000-4000-8000-080000002010', '2d000000-0000-4000-8000-070000002010', '2d000000-0000-4000-8000-040000002010', 1, 1.0, '2d000000-0000-4000-8000-090000002010'),
    ('2d000000-0000-4000-8000-080000002011', '2d000000-0000-4000-8000-070000002011', '2d000000-0000-4000-8000-040000002011', 1, 1.0, '2d000000-0000-4000-8000-090000002011'),
    ('2d000000-0000-4000-8000-080000002012', '2d000000-0000-4000-8000-070000002012', '2d000000-0000-4000-8000-040000002012', 1, 1.0, '2d000000-0000-4000-8000-090000002012')
ON CONFLICT DO NOTHING;
