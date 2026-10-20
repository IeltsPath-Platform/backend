-- The Practice of the Writing lessons W1 to W5 is writing, not multiple choice. Each lesson now offers two essay tasks
-- (a Task 1 report or a Task 2 essay, graded against the rubric like the other Writing practice). The short
-- multiple-choice and fill-in sets of V61 stay in the knowledge points' pool of review and probe material, so they are
-- unlinked from their lesson rather than deleted.

UPDATE content_packages SET lesson_id = NULL, topic_id = NULL, updated_at = CURRENT_TIMESTAMP
WHERE code LIKE 'PS-W_-LP-_';

CREATE TEMP TABLE writing_essay_tasks (
    pkg_code TEXT, pkg_title TEXT, lesson_id UUID, kp_code TEXT, stem TEXT, answer_spec JSONB, explanation TEXT, hint TEXT
) ON COMMIT DROP;

INSERT INTO writing_essay_tasks VALUES
-- W1: Task 1, describing charts
('PS-W1-WR-1', 'Viết bài: Biểu đồ cột (sở hữu ô tô)', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART',
 'The bar chart below shows the percentage of households in four countries that owned a car in 2000 and in 2020. Country A: 60% in 2000 and 72% in 2020. Country B: 45% and 58%. Country C: 30% and 55%. Country D: 70% and 68%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.',
 '{"type": "ESSAY", "task": "TASK_1", "minWords": 150, "passBand": 5.5, "chartFacts": "Car ownership (% of households), 2000 and 2020. A: 60 to 72. B: 45 to 58. C: 30 to 55. D: 70 to 68. Key features: ownership rose in countries A, B and C, most of all in C (up 25 points); D was the only country where it fell, slightly (down 2 points); D had the highest figure in 2000 (70%) and A the highest in 2020 (72%)."}',
 'Gợi ý bài mẫu: mở bài diễn đạt lại đề; overview nêu ba nước tăng và nước D giảm nhẹ; hai đoạn thân bài so sánh số liệu 2000 với 2020.',
 'Nhớ viết overview, nhóm số liệu theo xu hướng và không nêu ý kiến cá nhân.'),
('PS-W1-WR-2', 'Viết bài: Biểu đồ tròn (chi tiêu gia đình)', '25000000-0000-4000-8000-030000000001', 'DEMO_READING_W1_CHART',
 'The two pie charts below show how an average family spent its monthly income in 1990 and in 2020. Housing: 25% in 1990 and 35% in 2020. Food: 30% and 18%. Transport: 10% and 15%. Leisure: 15% and 20%. Other: 20% and 12%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.',
 '{"type": "ESSAY", "task": "TASK_1", "minWords": 150, "passBand": 5.5, "chartFacts": "Family spending as a percentage of income, 1990 and 2020. Housing 25 to 35, Food 30 to 18, Transport 10 to 15, Leisure 15 to 20, Other 20 to 12. Key features: food was the largest item in 1990 but housing was the largest in 2020; housing, transport and leisure all rose while food and other spending fell; food fell by 12 points, the biggest change."}',
 'Gợi ý bài mẫu: overview nêu chi tiêu nhà ở tăng thành khoản lớn nhất còn ăn uống giảm; một đoạn nói các khoản tăng, một đoạn nói các khoản giảm.',
 'So sánh hai năm và nêu khoản thay đổi lớn nhất.'),

-- W2: Task 2, opinion essays
('PS-W2-WR-1', 'Viết bài: Đại học miễn phí', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION',
 'Some people believe that university education should be free for all students. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: nêu quan điểm rõ ở mở bài; mỗi đoạn thân bài một lý do (ví dụ: cơ hội bình đẳng, gánh nặng ngân sách) kèm ví dụ; kết luận nhắc lại quan điểm.',
 'Giữ một quan điểm nhất quán từ mở bài đến kết luận.'),
('PS-W2-WR-2', 'Viết bài: Học ngoại ngữ từ tiểu học', '25000000-0000-4000-8000-030000000002', 'DEMO_READING_W2_OPINION',
 'Many people think that children should start learning a foreign language at primary school rather than at secondary school. Do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: lý do thường gặp là trẻ học phát âm tốt hơn và có nhiều thời gian; cân nhắc nêu một hạn chế rồi giải thích vì sao lợi ích lớn hơn.',
 'Mỗi đoạn thân bài bắt đầu bằng một câu chủ đề.'),

-- W3: introductions and topic sentences
('PS-W3-WR-1', 'Viết bài: Giao thông công cộng miễn phí', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE',
 'In some cities, public transport is free for everyone. Do the advantages of this outweigh the disadvantages? Give reasons for your answer and include any relevant examples. Write at least 250 words. Focus: open with a paraphrase of the task and a clear thesis statement, and begin each body paragraph with a topic sentence.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: mở bài hai câu (diễn đạt lại đề + nêu quan điểm); mỗi đoạn thân bài một câu chủ đề nêu ý chính trước khi đưa ví dụ.',
 'Không chép lại đề bài ở mở bài.'),
('PS-W3-WR-2', 'Viết bài: Làm việc tại nhà', '34000000-0000-4000-8000-030000000001', 'DEMO_WRITING_TOPIC_SENTENCE',
 'Some people say that working from home is better for employees than working in an office. Others disagree. Discuss both views and give your own opinion. Give reasons for your answer and include any relevant examples. Write at least 250 words. Focus: a paraphrased introduction, a clear opinion and a topic sentence for each body paragraph.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: một đoạn cho mỗi quan điểm, ý kiến riêng nêu ở mở bài và nhắc lại ở kết luận.',
 'Câu chủ đề phải nêu ý chính của cả đoạn.'),

-- W4: linking words and coherence
('PS-W4-WR-1', 'Viết bài: Mua sắm trực tuyến', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING',
 'Online shopping is replacing traditional shops in many countries. What are the causes of this trend and what are its effects? Give reasons for your answer and include any relevant examples. Write at least 250 words. Focus: link your ideas with a range of linking words for cause, result, contrast and example, without overusing them.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: dùng because, since, therefore, as a result, however, for instance đúng chức năng và đúng dấu câu.',
 'Từ nối phải đúng nghĩa; đừng dùng từ nối ở đầu mọi câu.'),
('PS-W4-WR-2', 'Viết bài: Đường sá và giao thông công cộng', '34000000-0000-4000-8000-030000000002', 'DEMO_WRITING_LINKING',
 'Some people think that governments should spend money on public transport rather than on building new roads. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples. Write at least 250 words. Focus: coherent paragraphs with accurate linking words.',
 '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}',
 'Gợi ý: nối lý do với ví dụ bằng for instance / such as, nối hệ quả bằng consequently / as a result.',
 'Kiểm tra mỗi từ nối có đúng chức năng không.'),

-- W5: describing trends
('PS-W5-WR-1', 'Viết bài: Đồ thị đường (khách tham quan công viên)', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE',
 'The line graph below shows the number of visitors, in thousands, to a national park in each month of one year. January: 20. February: 22. March: 35. April: 50. May: 70. June: 90. July: 110. August: 105. September: 60. October: 40. November: 25. December: 30. Summarise the information by selecting and reporting the main features. Write at least 150 words. Focus: describe the trends with accurate verbs, adverbs and prepositions.',
 '{"type": "ESSAY", "task": "TASK_1", "minWords": 150, "passBand": 5.5, "chartFacts": "Monthly visitors to a national park, in thousands. Jan 20, Feb 22, Mar 35, Apr 50, May 70, Jun 90, Jul 110, Aug 105, Sep 60, Oct 40, Nov 25, Dec 30. Key features: visitor numbers rose steadily from January to a peak of 110 thousand in July, fell sharply from August to November, and recovered slightly in December; the lowest figure was 20 thousand in January."}',
 'Gợi ý bài mẫu: overview nêu đỉnh vào tháng 7 và mức thấp vào mùa đông; đoạn 1 tăng từ tháng 1 đến tháng 7, đoạn 2 giảm từ tháng 8 đến tháng 12.',
 'Dùng rose, climbed, peaked at, fell sharply, declined và đủ giới từ.'),
('PS-W5-WR-2', 'Viết bài: Đồ thị đường (giá nhiên liệu)', '34000000-0000-4000-8000-030000000003', 'DEMO_WRITING_TREND_LANGUAGE',
 'The line graph below shows the average price of a litre of petrol and of diesel between 2015 and 2020, in dollars. Petrol: 1.10, 1.05, 1.20, 1.35, 1.30, 1.00. Diesel: 1.00, 0.95, 1.15, 1.40, 1.38, 1.05. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words. Focus: trend language for rises, falls and the highest point.',
 '{"type": "ESSAY", "task": "TASK_1", "minWords": 150, "passBand": 5.5, "chartFacts": "Average price per litre in dollars, 2015 to 2020. Petrol: 1.10, 1.05, 1.20, 1.35, 1.30, 1.00. Diesel: 1.00, 0.95, 1.15, 1.40, 1.38, 1.05. Key features: both fuels dipped in 2016, rose to a peak in 2018 (petrol 1.35, diesel 1.40) and then fell, ending in 2020 close to or below their 2015 levels (petrol 1.00, diesel 1.05); diesel was cheaper than petrol until 2017 and dearer in 2018 and 2019."}',
 'Gợi ý bài mẫu: overview nêu cả hai loại nhiên liệu cùng đạt đỉnh năm 2018 rồi giảm; so sánh petrol với diesel ở các năm.',
 'Nhớ nêu đúng mốc thời gian và so sánh hai đường.');

DO $$
DECLARE
    task RECORD;
    writing_topic CONSTANT UUID := '25000000-0000-4000-8000-010000000001';
    package_id UUID;
    version_id UUID;
    section_id UUID;
    question_id UUID;
    question_version_id UUID;
BEGIN
    FOR task IN SELECT * FROM writing_essay_tasks ORDER BY pkg_code LOOP
        package_id := gen_random_uuid();
        version_id := gen_random_uuid();
        section_id := gen_random_uuid();
        question_id := gen_random_uuid();
        question_version_id := gen_random_uuid();
        INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status)
        VALUES (package_id, task.pkg_code, task.pkg_title, 'PRACTICE_SET', writing_topic, task.lesson_id, 'PUBLISHED');
        INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
        VALUES (version_id, package_id, 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);
        UPDATE content_packages SET current_published_version_id = version_id WHERE id = package_id;
        INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
        VALUES (section_id, version_id, 'Bài viết', 'WRITING', 1);
        INSERT INTO questions (id, question_type, skill, purpose, status)
        VALUES (question_id, 'ESSAY', 'WRITING', 'LEARNING', 'PUBLISHED');
        INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version,
                                       explanation, hint, difficulty, status)
        VALUES (question_version_id, question_id, 1, task.stem, NULL, task.answer_spec, 1, task.explanation, task.hint,
                'MEDIUM', 'PUBLISHED');
        UPDATE questions SET current_published_version_id = question_version_id WHERE id = question_id;
        INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
        VALUES (question_version_id, (SELECT id FROM knowledge_points WHERE code = task.kp_code), 1.0);
        INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
        VALUES (gen_random_uuid(), section_id, question_version_id, 1, 1.0);
    END LOOP;
END $$;
