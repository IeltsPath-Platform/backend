-- The Writing chapter of IELTS 5.5 (topic DEMO_WRITING, lessons W1 to W5) gets the rest of its learning material:
--   * two practice sets of three questions for every lesson (the lesson's Practice tab lists the PRACTICE_SET packages
--     whose lesson_id is the lesson), and
--   * a final test for the chapter (TOPIC_TEST) of ten questions that covers all five knowledge points.
-- Every question is new: none is shared with a lesson exercise, a free-standing practice set or another test.

CREATE TEMP TABLE writing_material_items (
    pkg_code TEXT, pkg_title TEXT, pkg_kind TEXT, lesson_id UUID, kp_code TEXT, q_no INT, qtype TEXT, stem TEXT,
    opt_a TEXT, opt_b TEXT, opt_c TEXT, correct TEXT, accepted TEXT[], explanation TEXT, hint TEXT, difficulty TEXT
) ON COMMIT DROP;

INSERT INTO writing_material_items VALUES
-- W1: describing a chart (Task 1)
('PS-W1-LP-1', 'Luyện tập: Overview của biểu đồ', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 1, 'MULTIPLE_CHOICE',
 'What is the main purpose of the overview paragraph in Writing Task 1?',
 'To give every number in detail', 'To summarise the main trends or differences', 'To give a personal opinion on the data', 'B', NULL,
 'The overview gives the reader the big picture; details belong to the body paragraphs and opinions are not wanted in Task 1.',
 'Overview nói bức tranh chung, không đi vào chi tiết.', 'EASY'),
('PS-W1-LP-1', 'Luyện tập: Overview của biểu đồ', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 2, 'MULTIPLE_CHOICE',
 'Which sentence is the best overview for a bar chart about coffee consumption in four countries?',
 'Coffee is popular in many countries.',
 'Overall, Finland had the highest coffee consumption, while Japan had the lowest.',
 'In 2010, Finland drank 12 kg, Italy 5 kg, Brazil 6 kg and Japan 2 kg.', 'B', NULL,
 'B names the most important comparison without detailed figures. A is too vague and C only lists numbers.',
 'Overview nêu điểm nổi bật nhất, không liệt kê hết số liệu.', 'MEDIUM'),
('PS-W1-LP-1', 'Luyện tập: Overview của biểu đồ', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: Task 1 asks you to ______ the information shown in a chart, not to give your opinion.',
 NULL, NULL, NULL, NULL, ARRAY['describe', 'summarise', 'summarize', 'report'],
 'Task 1 is a factual report on the data, so the writer describes or summarises it.',
 'Từ cần điền nghĩa là trình bày lại thông tin một cách khách quan.', 'EASY'),
('PS-W1-LP-2', 'Luyện tập: So sánh số liệu', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 1, 'MULTIPLE_CHOICE',
 'Which sentence compares two figures correctly?',
 'Sales in 2010 were twice as high as in 2005.', 'Sales in 2010 are twice highly than 2005.', 'Sales in 2010 was more twice 2005.', 'A', NULL,
 'Twice as high as is a correct comparative structure; the other options contain grammar errors.',
 'Tìm cấu trúc so sánh đúng ngữ pháp.', 'MEDIUM'),
('PS-W1-LP-2', 'Luyện tập: So sánh số liệu', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 2, 'MULTIPLE_CHOICE',
 'Which plan is best for a Task 1 report about a line graph covering twenty years?',
 'Introduction, overview, then two body paragraphs grouped by trend', 'Introduction, opinion, conclusion', 'One long paragraph that lists every number', 'A', NULL,
 'A clear report has an introduction, an overview and body paragraphs that group related data.',
 'Task 1 không có phần ý kiến cá nhân.', 'MEDIUM'),
('PS-W1-LP-2', 'Luyện tập: So sánh số liệu', 'PRACTICE', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: In 2000, public transport use was much ______ than car use (25% compared with 60%).',
 NULL, NULL, NULL, NULL, ARRAY['lower', 'smaller', 'less'],
 'Twenty-five percent is smaller than sixty percent, so a comparative such as lower is needed.',
 'So sánh 25% với 60%.', 'EASY'),

-- W2: opinion essay (Task 2)
('PS-W2-LP-1', 'Luyện tập: Cấu trúc bài luận', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 1, 'MULTIPLE_CHOICE',
 'How many main body paragraphs does a standard opinion essay usually have?',
 'One', 'Two', 'Five', 'B', NULL,
 'Two body paragraphs, each with one main reason, fit the time and word limit of Task 2.',
 'Mỗi đoạn thân bài nêu một lý do chính.', 'EASY'),
('PS-W2-LP-1', 'Luyện tập: Cấu trúc bài luận', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 2, 'MULTIPLE_CHOICE',
 'Which sentence is a clear thesis statement for the question "Should schools ban mobile phones?"',
 'Mobile phones are used by students.',
 'I strongly believe that schools should ban mobile phones because they distract students.',
 'This essay will discuss phones.', 'B', NULL,
 'B states a position and a reason. A states a fact and C announces the topic only.',
 'Thesis statement nêu rõ quan điểm của bạn.', 'MEDIUM'),
('PS-W2-LP-1', 'Luyện tập: Cấu trúc bài luận', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: each body paragraph should begin with a topic ______ that states its main point.',
 NULL, NULL, NULL, NULL, ARRAY['sentence'],
 'A topic sentence opens the paragraph and states its main point.',
 'Từ cần điền là một đơn vị của đoạn văn.', 'EASY'),
('PS-W2-LP-2', 'Luyện tập: Lý do và ví dụ', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 1, 'MULTIPLE_CHOICE',
 'Which sentence is the best example to support the idea that cycling improves health?',
 'Cycling is good.',
 'For instance, adults who cycle to work have lower rates of heart disease than those who drive.',
 'I like cycling at weekends.', 'B', NULL,
 'B gives specific evidence that supports the idea. A is vague and C is only a personal preference.',
 'Ví dụ tốt cụ thể và chứng minh được ý chính.', 'MEDIUM'),
('PS-W2-LP-2', 'Luyện tập: Lý do và ví dụ', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 2, 'MULTIPLE_CHOICE',
 'Which sentence is the best way to begin a conclusion?',
 'Thank you for reading my essay.',
 'In conclusion, although there are some drawbacks, I believe the benefits of cycling to work outweigh them.',
 'In conclusion, a new idea about trains is that they are fast.', 'B', NULL,
 'A conclusion restates the writer''s position and sums up the reasons; it adds no new idea.',
 'Kết luận nhắc lại quan điểm, không thêm ý mới.', 'MEDIUM'),
('PS-W2-LP-2', 'Luyện tập: Lý do và ví dụ', 'PRACTICE', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: in the conclusion you should ______ your opinion rather than introduce a new idea.',
 NULL, NULL, NULL, NULL, ARRAY['restate', 'repeat', 'reaffirm', 'summarise', 'summarize'],
 'The conclusion goes back to the opinion given in the introduction.',
 'Từ cần điền nghĩa là nhắc lại.', 'MEDIUM'),

-- W3: introduction and topic sentences
('PS-W3-LP-1', 'Luyện tập: Mở bài', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 1, 'MULTIPLE_CHOICE',
 'Which is the best opening for the task "Some people think that homework should be abolished."?',
 'Some people think that homework should be abolished.',
 'It is argued by certain people that setting homework for pupils ought to be stopped.',
 'Homework is a topic.', 'B', NULL,
 'B restates the task with new words and structure. A copies it and C does not restate it.',
 'Diễn đạt lại đề bằng cấu trúc khác.', 'MEDIUM'),
('PS-W3-LP-1', 'Luyện tập: Mở bài', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 2, 'MULTIPLE_CHOICE',
 'What should an introduction NOT contain?',
 'A paraphrase of the task', 'A thesis statement', 'Detailed examples and statistics', 'C', NULL,
 'Examples and statistics belong to the body paragraphs; the introduction only restates the task and states the position.',
 'Mở bài ngắn, chỉ có hai việc cần làm.', 'EASY'),
('PS-W3-LP-1', 'Luyện tập: Mở bài', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: a strong introduction has a paraphrase of the task and a ______ statement.',
 NULL, NULL, NULL, NULL, ARRAY['thesis'],
 'The thesis statement gives the writer''s position on the question.',
 'Câu nêu quan điểm chính của bài luận.', 'MEDIUM'),
('PS-W3-LP-2', 'Luyện tập: Câu chủ đề', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 1, 'MULTIPLE_CHOICE',
 'Which is the best topic sentence for a paragraph about the drawbacks of social media?',
 'Social media can harm young people because it reduces face-to-face contact and affects their sleep.',
 'Yesterday I spent three hours on social media.', 'Social media.', 'A', NULL,
 'A states a main idea with reasons that the paragraph can develop. B is a personal detail and C is not a sentence.',
 'Câu chủ đề nêu ý chính kèm lý do.', 'EASY'),
('PS-W3-LP-2', 'Luyện tập: Câu chủ đề', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 2, 'MULTIPLE_CHOICE',
 'A good topic sentence should be...',
 'as long as possible',
 'general enough to cover the paragraph but clear enough to make one point',
 'a question to the reader', 'B', NULL,
 'It must control the whole paragraph while still making one clear point.',
 'Không quá rộng, không quá hẹp.', 'MEDIUM'),
('PS-W3-LP-2', 'Luyện tập: Câu chủ đề', 'PRACTICE', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: examples and ______ come after the topic sentence to support it.',
 NULL, NULL, NULL, NULL, ARRAY['evidence', 'details', 'reasons', 'explanations'],
 'After the main idea the paragraph needs support such as evidence, details or explanations.',
 'Phần hỗ trợ cho ý chính.', 'MEDIUM'),

-- W4: linking words and coherence
('PS-W4-LP-1', 'Luyện tập: Chọn từ nối', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 1, 'MULTIPLE_CHOICE',
 'Choose the best word: "The city has good public transport. ______, many residents still prefer cars."',
 'Therefore', 'However', 'Similarly', 'B', NULL,
 'The second sentence goes against what we expect, so a contrast word is needed.',
 'Hai câu nói điều trái ngược.', 'EASY'),
('PS-W4-LP-1', 'Luyện tập: Chọn từ nối', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 2, 'MULTIPLE_CHOICE',
 'Which sentence uses "whereas" correctly?',
 'Whereas he was tired, he went to bed.',
 'Urban areas are crowded, whereas rural areas are quiet.',
 'Whereas of the rain, we stayed home.', 'B', NULL,
 'Whereas joins two contrasting clauses. The other sentences do not contrast two ideas.',
 'Whereas nối hai vế đối lập.', 'MEDIUM'),
('PS-W4-LP-1', 'Luyện tập: Chọn từ nối', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Students are under great pressure; ______, many of them feel stressed."',
 NULL, NULL, NULL, NULL, ARRAY['consequently', 'therefore', 'thus', 'hence'],
 'Stress is a result of the pressure, so a result word is needed.',
 'Căng thẳng là kết quả của áp lực.', 'MEDIUM'),
('PS-W4-LP-2', 'Luyện tập: Mạch lạc', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 1, 'MULTIPLE_CHOICE',
 'Which pair of sentences is the most coherent?',
 'Exercise is healthy. Moreover, it improves mood.',
 'Exercise is healthy. However, it improves mood.',
 'Exercise is healthy. Therefore, mountains are high.', 'A', NULL,
 'Moreover adds a related benefit. However would wrongly signal a contrast and the last pair is unrelated.',
 'Hai câu cùng nói về lợi ích của tập thể dục.', 'MEDIUM'),
('PS-W4-LP-2', 'Luyện tập: Mạch lạc', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 2, 'MULTIPLE_CHOICE',
 'Using "firstly, secondly, moreover" at the start of every sentence is...',
 'recommended for a high band', 'a weakness because the text sounds mechanical', 'required in Task 2', 'B', NULL,
 'Coherence comes from the logic of the ideas; overusing linking words makes the writing unnatural.',
 'Mạch lạc đến từ logic của ý, không phải số từ nối.', 'MEDIUM'),
('PS-W4-LP-2', 'Luyện tập: Mạch lạc', 'PRACTICE', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Cars are expensive to run. ______ instance, fuel and insurance cost thousands each year."',
 NULL, NULL, NULL, NULL, ARRAY['for'],
 'For instance introduces an example that supports the previous sentence.',
 'Cụm từ nối có nghĩa là ví dụ.', 'EASY'),

-- W5: describing trends (Task 1)
('PS-W5-LP-1', 'Luyện tập: Mô tả đường biểu diễn', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 1, 'MULTIPLE_CHOICE',
 'Which sentence describes a gradual decrease?',
 'Sales shot up.', 'Sales declined gradually.', 'Sales stayed flat.', 'B', NULL,
 'Declined means went down and gradually shows a slow change.',
 'Chọn câu chỉ sự giảm chậm.', 'EASY'),
('PS-W5-LP-1', 'Luyện tập: Mô tả đường biểu diễn', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 2, 'MULTIPLE_CHOICE',
 'Which expression is closest in meaning to "plummet"?',
 'rise slowly', 'fall sharply', 'stay stable', 'B', NULL,
 'Plummet means to fall very quickly and by a large amount.',
 'Từ này chỉ sự giảm mạnh.', 'MEDIUM'),
('PS-W5-LP-1', 'Luyện tập: Mô tả đường biểu diễn', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Between 2000 and 2010 the figure rose ______ 20% to 35%."',
 NULL, NULL, NULL, NULL, ARRAY['from'],
 'Rose from 20% to 35% gives the starting and the end values.',
 'Giới từ chỉ giá trị bắt đầu.', 'EASY'),
('PS-W5-LP-2', 'Luyện tập: Viết câu số liệu', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 1, 'MULTIPLE_CHOICE',
 'Which sentence is correct?',
 'The number of tourists raised to 5 million in 2015.',
 'The number of tourists rose to 5 million in 2015.',
 'The number of tourists rised to 5 million in 2015.', 'B', NULL,
 'Rose is the past tense of rise, an intransitive verb. Raised needs an object and rised does not exist.',
 'Rise là động từ bất quy tắc.', 'MEDIUM'),
('PS-W5-LP-2', 'Luyện tập: Viết câu số liệu', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 2, 'MULTIPLE_CHOICE',
 'Which sentence includes both a trend and a time period?',
 'Exports fell sharply between 2008 and 2010.', 'Exports fell sharply.', 'Exports in 2010.', 'A', NULL,
 'Only the first sentence gives the direction, the degree and the period.',
 'Cần có xu hướng và mốc thời gian.', 'EASY'),
('PS-W5-LP-2', 'Luyện tập: Viết câu số liệu', 'PRACTICE', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "Exports peaked ______ 90 billion dollars in 2012."',
 NULL, NULL, NULL, NULL, ARRAY['at'],
 'Peaked at gives the highest value reached.',
 'Giới từ đi với peak.', 'MEDIUM'),

-- Final test of the chapter: two questions for each of the five knowledge points
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_READING_W1_CHART', 1, 'MULTIPLE_CHOICE',
 'A pie chart shows how a family spends its monthly income. Which sentence is the best overview?',
 'The family spends 30% on food and 20% on rent.',
 'Overall, housing and food take up the largest shares of the family budget, while entertainment takes the smallest.',
 'I think the family spends too much on food.', 'B', NULL,
 'The overview summarises the main comparison without detailed figures or opinions.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_READING_W1_CHART', 2, 'FILL_IN_BLANK',
 'Complete with ONE WORD: in academic Writing Task 1 you write a ______ of at least 150 words about the data.',
 NULL, NULL, NULL, NULL, ARRAY['report', 'summary', 'description'],
 'Task 1 is a factual report or summary of the visual information.', NULL, 'EASY'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_READING_W2_OPINION', 3, 'MULTIPLE_CHOICE',
 'In an opinion essay, where should you state your position?',
 'Only in the conclusion', 'In the introduction and again in the conclusion', 'In the middle paragraph only', 'B', NULL,
 'A consistent position stated at the start and repeated at the end gives a clear line of argument.', NULL, 'EASY'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_READING_W2_OPINION', 4, 'FILL_IN_BLANK',
 'Complete with ONE WORD: each body paragraph should contain one main ______ supported by an example.',
 NULL, NULL, NULL, NULL, ARRAY['idea', 'point', 'reason', 'argument'],
 'A paragraph develops one main idea, point or reason with support.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_TOPIC_SENTENCE', 5, 'MULTIPLE_CHOICE',
 'Which sentence is a topic sentence?',
 'Public libraries provide free access to information, which helps people on all incomes to learn.',
 'The library near my house has 5,000 books.',
 'For instance, I borrowed a book last week.', 'A', NULL,
 'A states a general idea with a reason; B and C are details and an example.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_TOPIC_SENTENCE', 6, 'MULTIPLE_CHOICE',
 'Which is the best paraphrase of "Young people spend too much time online."?',
 'Young people spend too much time online.',
 'Teenagers and young adults devote an excessive amount of their time to the internet.',
 'Young people use computers.', 'B', NULL,
 'B keeps the meaning with new words and structure; A copies and C changes the meaning.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_LINKING', 7, 'MULTIPLE_CHOICE',
 'Choose the best word: "Plastic bags are cheap. ______, they damage the environment."',
 'For example', 'However', 'Firstly', 'B', NULL,
 'The second sentence contrasts with the first, so however is needed.', NULL, 'EASY'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_LINKING', 8, 'FILL_IN_BLANK',
 'Complete with ONE WORD that shows a result: "Many students work part-time. ______, they have less time to study."',
 NULL, NULL, NULL, NULL, ARRAY['consequently', 'therefore', 'thus', 'hence'],
 'Less study time is a result of working part-time.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_TREND_LANGUAGE', 9, 'MULTIPLE_CHOICE',
 'Which sentence is correct?',
 'The price rised to $50.', 'The price rose to $50 in June.', 'The price raised to $50.', 'B', NULL,
 'Rose is the past tense of rise, which has no object.', NULL, 'MEDIUM'),
('WT-WRITING-1', 'Đề cuối chặng: Writing cơ bản', 'TEST', NULL, 'DEMO_WRITING_TREND_LANGUAGE', 10, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "The number of users ______ stable at 2,000 throughout the period."',
 NULL, NULL, NULL, NULL, ARRAY['remained', 'stayed'],
 'Remained stable means the figure did not change.', NULL, 'MEDIUM');

DO $$
DECLARE
    pkg RECORD;
    item RECORD;
    writing_topic CONSTANT UUID := '25000000-0000-4000-8000-010000000001';
    package_id UUID;
    version_id UUID;
    section_id UUID;
    question_id UUID;
    question_version_id UUID;
BEGIN
    FOR pkg IN SELECT DISTINCT pkg_code, pkg_title, pkg_kind, lesson_id FROM writing_material_items ORDER BY pkg_code LOOP
        package_id := gen_random_uuid();
        version_id := gen_random_uuid();
        section_id := gen_random_uuid();
        INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status)
        VALUES (package_id, pkg.pkg_code, pkg.pkg_title,
                CASE pkg.pkg_kind WHEN 'TEST' THEN 'TOPIC_TEST' ELSE 'PRACTICE_SET' END,
                writing_topic, pkg.lesson_id, 'PUBLISHED');
        INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
        VALUES (version_id, package_id, 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);
        UPDATE content_packages SET current_published_version_id = version_id WHERE id = package_id;
        INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
        VALUES (section_id, version_id, pkg.pkg_title, 'WRITING', 1);

        FOR item IN SELECT * FROM writing_material_items WHERE pkg_code = pkg.pkg_code ORDER BY q_no LOOP
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
            VALUES (question_version_id, (SELECT id FROM knowledge_points WHERE code = item.kp_code), 1.0);
            INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
            VALUES (gen_random_uuid(), section_id, question_version_id, item.q_no, 1.0);
        END LOOP;
    END LOOP;
END $$;
