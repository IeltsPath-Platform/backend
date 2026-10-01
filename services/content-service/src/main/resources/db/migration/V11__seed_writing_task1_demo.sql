-- Writing Task 1 Academic demo: a chart-description knowledge point (KP6), one essay question with its bar chart
-- (an SVG data URI) and an essay block at the end of L3. The grader reads chartFacts from the answer spec instead of
-- the image; chartFacts and the model answer only leave Content through the internal API.

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('22000000-0000-4000-8000-020000000006', '10000000-0000-4000-8000-000000000001', 'DEMO_READING_W1_CHART',
     'Mô tả biểu đồ (Task 1)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Mô tả biểu đồ: mở bằng câu tổng quan nêu xu hướng chính, rồi dẫn số liệu và so sánh để chứng minh.',
     'ACTIVE');

INSERT INTO questions (id, question_type, skill, status) VALUES
    ('22000000-0000-4000-8000-060000000001', 'ESSAY', 'WRITING', 'PUBLISHED');
INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('22000000-0000-4000-8000-070000000001', '22000000-0000-4000-8000-060000000001', 1,
     'The bar chart below shows the average afternoon surface temperature of three types of roof in three cities in July. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.',
     NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":6.0,"chartFacts":"Average afternoon roof surface temperature in July (°C). Madrid: black roof 82, white-painted roof 45, green (planted) roof 33. Chicago: black roof 74, white-painted roof 41, green (planted) roof 30. Copenhagen: black roof 61, white-painted roof 35, green (planted) roof 25. Gap between black and green roofs: Madrid 49, Chicago 44, Copenhagen 36. Key features: black roofs hottest and green roofs coolest in every city; Madrid hottest and Copenhagen coolest for every roof type; white roofs about 45% cooler than black roofs in each city."}', 1,
     'The chart compares how hot three kinds of roof became on July afternoons in Madrid, Chicago and Copenhagen.

It is clear that the type of roof mattered far more than the location. In every city, black roofs were the hottest and planted roofs the coolest, with white roofs in between. Madrid was the hottest city for every roof type, and Copenhagen the mildest.

Black roofs peaked at 82°C in Madrid and reached 74°C in Chicago and 61°C in Copenhagen. Painting a roof white cut these figures to 45°C, 41°C and 35°C respectively, a fall of roughly 45 per cent in each case.

Green roofs performed best, staying at 33°C in Madrid, 30°C in Chicago and just 25°C in Copenhagen. The advantage of planting over black roofing was therefore greatest in Madrid, at 49°C, compared with 44°C in Chicago and 36°C in Copenhagen.',
     'PUBLISHED');
UPDATE questions SET current_published_version_id = '22000000-0000-4000-8000-070000000001' WHERE id = '22000000-0000-4000-8000-060000000001';
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('22000000-0000-4000-8000-070000000001', '22000000-0000-4000-8000-020000000006', 1.00);

-- The chart the learner sees; its figures match chartFacts.
INSERT INTO content_assets (id, asset_type, text_content, media_reference, validation_status) VALUES
    ('22000000-0000-4000-8000-050000000001', 'IMAGE', 'Bar chart of July afternoon roof temperatures for black, white and green roofs in Madrid, Chicago and Copenhagen.',
     'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI1NjAiIGhlaWdodD0iMzQwIiB2aWV3Qm94PSIwIDAgNTYwIDM0MCIgZm9udC1mYW1pbHk9IkFyaWFsLHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTIiPjxyZWN0IHdpZHRoPSI1NjAiIGhlaWdodD0iMzQwIiBmaWxsPSIjZmZmIi8+PHRleHQgeD0iMjgwIiB5PSIyMiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIxNCIgZm9udC13ZWlnaHQ9ImJvbGQiPkF2ZXJhZ2UgYWZ0ZXJub29uIHJvb2Ygc3VyZmFjZSB0ZW1wZXJhdHVyZSwgSnVseSAowrBDKTwvdGV4dD48bGluZSB4MT0iNTAiIHkxPSIyODAuMCIgeDI9IjU0MCIgeTI9IjI4MC4wIiBzdHJva2U9IiNlNWU1ZTUiLz48dGV4dCB4PSI0NCIgeT0iMjg0LjAiIHRleHQtYW5jaG9yPSJlbmQiPjA8L3RleHQ+PGxpbmUgeDE9IjUwIiB5MT0iMjQwLjAiIHgyPSI1NDAiIHkyPSIyNDAuMCIgc3Ryb2tlPSIjZTVlNWU1Ii8+PHRleHQgeD0iNDQiIHk9IjI0NC4wIiB0ZXh0LWFuY2hvcj0iZW5kIj4xNTwvdGV4dD48bGluZSB4MT0iNTAiIHkxPSIyMDAuMCIgeDI9IjU0MCIgeTI9IjIwMC4wIiBzdHJva2U9IiNlNWU1ZTUiLz48dGV4dCB4PSI0NCIgeT0iMjA0LjAiIHRleHQtYW5jaG9yPSJlbmQiPjMwPC90ZXh0PjxsaW5lIHgxPSI1MCIgeTE9IjE2MC4wIiB4Mj0iNTQwIiB5Mj0iMTYwLjAiIHN0cm9rZT0iI2U1ZTVlNSIvPjx0ZXh0IHg9IjQ0IiB5PSIxNjQuMCIgdGV4dC1hbmNob3I9ImVuZCI+NDU8L3RleHQ+PGxpbmUgeDE9IjUwIiB5MT0iMTIwLjAiIHgyPSI1NDAiIHkyPSIxMjAuMCIgc3Ryb2tlPSIjZTVlNWU1Ii8+PHRleHQgeD0iNDQiIHk9IjEyNC4wIiB0ZXh0LWFuY2hvcj0iZW5kIj42MDwvdGV4dD48bGluZSB4MT0iNTAiIHkxPSI4MC4wIiB4Mj0iNTQwIiB5Mj0iODAuMCIgc3Ryb2tlPSIjZTVlNWU1Ii8+PHRleHQgeD0iNDQiIHk9Ijg0LjAiIHRleHQtYW5jaG9yPSJlbmQiPjc1PC90ZXh0PjxsaW5lIHgxPSI1MCIgeTE9IjQwLjAiIHgyPSI1NDAiIHkyPSI0MC4wIiBzdHJva2U9IiNlNWU1ZTUiLz48dGV4dCB4PSI0NCIgeT0iNDQuMCIgdGV4dC1hbmNob3I9ImVuZCI+OTA8L3RleHQ+PHJlY3QgeD0iNzQuNyIgeT0iNjEuMyIgd2lkdGg9IjM0IiBoZWlnaHQ9IjIxOC43IiBmaWxsPSIjMzMzMzMzIi8+PHRleHQgeD0iOTEuNyIgeT0iNTcuMyIgdGV4dC1hbmNob3I9Im1pZGRsZSI+ODI8L3RleHQ+PHJlY3QgeD0iMTE0LjciIHk9IjE2MC4wIiB3aWR0aD0iMzQiIGhlaWdodD0iMTIwLjAiIGZpbGw9IiNiOGI4YjgiLz48dGV4dCB4PSIxMzEuNyIgeT0iMTU2LjAiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjQ1PC90ZXh0PjxyZWN0IHg9IjE1NC43IiB5PSIxOTIuMCIgd2lkdGg9IjM0IiBoZWlnaHQ9Ijg4LjAiIGZpbGw9IiM0YzlhMmEiLz48dGV4dCB4PSIxNzEuNyIgeT0iMTg4LjAiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjMzPC90ZXh0Pjx0ZXh0IHg9IjEzMS43IiB5PSIyOTgiIHRleHQtYW5jaG9yPSJtaWRkbGUiPk1hZHJpZDwvdGV4dD48cmVjdCB4PSIyMzguMCIgeT0iODIuNyIgd2lkdGg9IjM0IiBoZWlnaHQ9IjE5Ny4zIiBmaWxsPSIjMzMzMzMzIi8+PHRleHQgeD0iMjU1LjAiIHk9Ijc4LjciIHRleHQtYW5jaG9yPSJtaWRkbGUiPjc0PC90ZXh0PjxyZWN0IHg9IjI3OC4wIiB5PSIxNzAuNyIgd2lkdGg9IjM0IiBoZWlnaHQ9IjEwOS4zIiBmaWxsPSIjYjhiOGI4Ii8+PHRleHQgeD0iMjk1LjAiIHk9IjE2Ni43IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj40MTwvdGV4dD48cmVjdCB4PSIzMTguMCIgeT0iMjAwLjAiIHdpZHRoPSIzNCIgaGVpZ2h0PSI4MC4wIiBmaWxsPSIjNGM5YTJhIi8+PHRleHQgeD0iMzM1LjAiIHk9IjE5Ni4wIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj4zMDwvdGV4dD48dGV4dCB4PSIyOTUuMCIgeT0iMjk4IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj5DaGljYWdvPC90ZXh0PjxyZWN0IHg9IjQwMS4zIiB5PSIxMTcuMyIgd2lkdGg9IjM0IiBoZWlnaHQ9IjE2Mi43IiBmaWxsPSIjMzMzMzMzIi8+PHRleHQgeD0iNDE4LjMiIHk9IjExMy4zIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj42MTwvdGV4dD48cmVjdCB4PSI0NDEuMyIgeT0iMTg2LjciIHdpZHRoPSIzNCIgaGVpZ2h0PSI5My4zIiBmaWxsPSIjYjhiOGI4Ii8+PHRleHQgeD0iNDU4LjMiIHk9IjE4Mi43IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj4zNTwvdGV4dD48cmVjdCB4PSI0ODEuMyIgeT0iMjEzLjMiIHdpZHRoPSIzNCIgaGVpZ2h0PSI2Ni43IiBmaWxsPSIjNGM5YTJhIi8+PHRleHQgeD0iNDk4LjMiIHk9IjIwOS4zIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj4yNTwvdGV4dD48dGV4dCB4PSI0NTguMyIgeT0iMjk4IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj5Db3BlbmhhZ2VuPC90ZXh0PjxsaW5lIHgxPSI1MCIgeTE9IjI4MCIgeDI9IjU0MCIgeTI9IjI4MCIgc3Ryb2tlPSIjMDAwIi8+PHJlY3QgeD0iNTAiIHk9IjMxNCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIiBmaWxsPSIjMzMzMzMzIi8+PHRleHQgeD0iNjYiIHk9IjMyNCI+QmxhY2sgcm9vZjwvdGV4dD48cmVjdCB4PSIxNDUuMCIgeT0iMzE0IiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiIGZpbGw9IiNiOGI4YjgiLz48dGV4dCB4PSIxNjEuMCIgeT0iMzI0Ij5XaGl0ZS1wYWludGVkIHJvb2Y8L3RleHQ+PHJlY3QgeD0iMjkyLjAiIHk9IjMxNCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIiBmaWxsPSIjNGM5YTJhIi8+PHRleHQgeD0iMzA4LjAiIHk9IjMyNCI+R3JlZW4gKHBsYW50ZWQpIHJvb2Y8L3RleHQ+PC9zdmc+',
     'VALID');
INSERT INTO content_asset_links (asset_id, question_version_id, sort_order) VALUES
    ('22000000-0000-4000-8000-050000000001', '22000000-0000-4000-8000-070000000001', 1);

-- L3: a TEXT block linking reading to Task 1, then the essay block.
INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES ('20000000-0000-4000-8000-030000000003', '22000000-0000-4000-8000-020000000006');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type, text_content) VALUES
    ('22000000-0000-4000-8000-040000000001', '20000000-0000-4000-8000-030000000003', 4, 'TEXT',
     'Viết cũng vậy: Writing Task 1 mở bằng câu tổng quan (ý chính của biểu đồ), rồi mới đưa số liệu (chi tiết) để chứng minh.');
INSERT INTO lesson_blocks (id, lesson_id, sort_order, block_type) VALUES
    ('22000000-0000-4000-8000-040000000002', '20000000-0000-4000-8000-030000000003', 5, 'EXERCISE');
INSERT INTO lesson_block_questions (block_id, question_version_id, sort_order) VALUES
    ('22000000-0000-4000-8000-040000000002', '22000000-0000-4000-8000-070000000001', 1);
