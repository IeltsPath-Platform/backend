-- Demo practice material for the five rubric-criterion knowledge points of V69: three free-standing practice sets of
-- three questions each per point, so a learner who is weak in one criterion has sets to review. The sets belong to no
-- lesson or topic, so they gate nothing. This is AI-authored demo content: it requires review by an IELTS-qualified
-- examiner before it is shown to learners as definitive teaching material.
CREATE TEMP TABLE writing_criterion_items (
    kp_code TEXT, pkg_code TEXT, pkg_title TEXT, q_no INT, qtype TEXT, stem TEXT,
    opt_a TEXT, opt_b TEXT, opt_c TEXT, correct TEXT, accepted TEXT[], explanation TEXT, hint TEXT, difficulty TEXT
) ON COMMIT DROP;

INSERT INTO writing_criterion_items VALUES
-- Task Achievement (Task 1)
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-1', 'Luyện thêm: Đoạn tổng quan (Task 1)', 1, 'MULTIPLE_CHOICE',
 'A bar chart shows that car ownership rose in all three countries between 2010 and 2020, and Country A stayed the highest. Which sentence is the best overview?',
 'Overall, car ownership rose in all three countries, and Country A remained the highest throughout.',
 'In 2010, 20% of people in Country A owned a car.', 'The bar chart is about cars in three countries.', 'A', NULL,
 'A summarises the main trend and the most striking feature without detailed figures. B is a single detail and C only names the topic.',
 'Tổng quan nêu xu hướng chính, không đi vào số liệu cụ thể.', 'EASY'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-1', 'Luyện thêm: Đoạn tổng quan (Task 1)', 2, 'MULTIPLE_CHOICE',
 'What should the overview paragraph of a Task 1 report contain?',
 'The main trends or the most striking features, without detailed figures', 'The writer''s personal opinion about the data',
 'Every figure in the chart, in order', 'A', NULL,
 'The overview gives the big picture. Opinions do not belong in Task 1, and listing every figure is detail rather than overview.',
 'Bức tranh chung, không phải từng con số.', 'EASY'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-1', 'Luyện thêm: Đoạn tổng quan (Task 1)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: an overview summarises the main ______ shown in the chart.',
 NULL, NULL, NULL, NULL, ARRAY['trends', 'trend', 'features', 'patterns', 'changes'],
 'The overview reports the main trends or features that are visible in the data.',
 'Từ cần điền nói về xu hướng hoặc đặc điểm chính của biểu đồ.', 'EASY'),

('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-2', 'Luyện thêm: Chọn và so sánh đặc điểm chính (Task 1)', 1, 'MULTIPLE_CHOICE',
 'A pie chart shows how a family spends its income on six items. Which approach is best?',
 'Describe all six items one by one with their percentages', 'Select the largest and smallest items and compare them',
 'Explain why the family spends money this way', 'B', NULL,
 'Task 1 asks you to select and report the main features and to compare where relevant. Listing everything is too detailed, and explaining reasons is not required.',
 'Chọn điểm nổi bật nhất rồi so sánh.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-2', 'Luyện thêm: Chọn và so sánh đặc điểm chính (Task 1)', 2, 'MULTIPLE_CHOICE',
 'Which sentence makes a clear comparison?',
 'Housing accounted for 35% of spending, compared with only 10% for transport.',
 'Housing is expensive, which worries many families.', 'There is a section for housing and a section for transport.', 'A', NULL,
 'A puts two figures side by side. B gives an opinion and C describes the layout, not the data.',
 'Câu so sánh đặt hai số liệu cạnh nhau.', 'EASY'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-2', 'Luyện thêm: Chọn và so sánh đặc điểm chính (Task 1)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: in Task 1 you should select and ______ the main features and make comparisons where relevant.',
 NULL, NULL, NULL, NULL, ARRAY['report', 'describe', 'summarise', 'summarize', 'highlight'],
 'The task wording is "select and report the main features". You choose what matters, then report it.',
 'Động từ nghĩa là trình bày lại những gì thấy trong biểu đồ.', 'MEDIUM'),

('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-3', 'Luyện thêm: Số liệu chính xác và đủ độ dài (Task 1)', 1, 'MULTIPLE_CHOICE',
 'A line graph shows sales rising from 30 units to 45 units. Which sentence reports this accurately?',
 'Sales rose from 30 to 45 units.', 'Sales doubled from 30 to 45 units.', 'Sales fell from 45 to 30 units.', 'A', NULL,
 'A matches the data. 45 is 1.5 times 30, not double, and C reverses the trend.',
 'Kiểm tra lại hướng và mức thay đổi của số liệu.', 'EASY'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-3', 'Luyện thêm: Số liệu chính xác và đủ độ dài (Task 1)', 2, 'MULTIPLE_CHOICE',
 'Which sentence should NOT appear in a Task 1 report?',
 'Sales peaked in July.', 'The company should spend more on advertising.', 'Sales were lowest in January.', 'B', NULL,
 'Task 1 reports the data. A recommendation is an opinion that the chart does not support. A and C report facts.',
 'Task 1 chỉ mô tả dữ liệu, không đưa ý kiến.', 'EASY'),
('DEMO_WRITING_CRITERION_TA', 'PS-WCR-TA-3', 'Luyện thêm: Số liệu chính xác và đủ độ dài (Task 1)', 3, 'FILL_IN_BLANK',
 'Complete with a NUMBER: a Task 1 report should contain at least ______ words.',
 NULL, NULL, NULL, NULL, ARRAY['150', 'one hundred and fifty', 'one hundred fifty'],
 'Task 1 requires a minimum of 150 words; shorter answers lose marks for Task Achievement.',
 'Số từ tối thiểu của Task 1.', 'EASY'),

-- Task Response (Task 2)
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-1', 'Luyện thêm: Trả lời đủ mọi phần của đề (Task 2)', 1, 'MULTIPLE_CHOICE',
 'The question says: "Some people think A. Others think B. Discuss both views and give your opinion." Which plan answers every part?',
 'Discuss only view A in detail', 'Discuss view A, discuss view B, then state and support your own opinion',
 'Write generally about the topic without mentioning either view', 'B', NULL,
 'The question has three parts: view A, view B and your opinion. Leaving out any part lowers Task Response.',
 'Đếm xem đề yêu cầu bao nhiêu phần.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-1', 'Luyện thêm: Trả lời đủ mọi phần của đề (Task 2)', 2, 'MULTIPLE_CHOICE',
 'Which question type asks you to say how far you agree?',
 '"To what extent do you agree or disagree?"', '"What are the causes of this problem?"', '"Discuss both views."', 'A', NULL,
 'To what extent do you agree or disagree asks for your position and its strength. The other forms ask for causes or for a discussion of views.',
 'Câu hỏi này đòi hỏi mức độ đồng ý của bạn.', 'EASY'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-1', 'Luyện thêm: Trả lời đủ mọi phần của đề (Task 2)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: the IELTS criterion that checks whether you answer every part of the question is called Task ______.',
 NULL, NULL, NULL, NULL, ARRAY['response'],
 'Task Response is the criterion for Writing Task 2; for Task 1 it is called Task Achievement.',
 'Tiêu chí Task 2 có tên viết tắt TR.', 'EASY'),

('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-2', 'Luyện thêm: Quan điểm rõ ràng và nhất quán (Task 2)', 1, 'MULTIPLE_CHOICE',
 'For the question "To what extent do you agree that cars should be banned from city centres?", which thesis statement is clearest?',
 'Cars and cities are often discussed by experts.',
 'I strongly agree that banning cars from city centres would improve air quality and public safety.',
 'Some people agree and some people disagree with this idea.', 'B', NULL,
 'B takes a clear position and previews the reasons. A names the topic only and C reports views without choosing one.',
 'Câu luận điểm cần cho biết bạn đứng về phía nào.', 'EASY'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-2', 'Luyện thêm: Quan điểm rõ ràng và nhất quán (Task 2)', 2, 'MULTIPLE_CHOICE',
 'A writer says "I agree" in the introduction but "I disagree" in the conclusion. What is the problem?',
 'The position is inconsistent', 'The essay is too formal', 'The essay has too many examples', 'A', NULL,
 'A position must stay the same from the introduction to the conclusion; changing it confuses the reader.',
 'So sánh quan điểm ở mở bài và kết bài.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-2', 'Luyện thêm: Quan điểm rõ ràng và nhất quán (Task 2)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: your position must stay ______ from the introduction to the conclusion.',
 NULL, NULL, NULL, NULL, ARRAY['consistent', 'clear', 'constant', 'unchanged', 'same'],
 'A consistent position keeps the argument focused and easy to follow.',
 'Từ cần điền nghĩa là không thay đổi.', 'EASY'),

('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-3', 'Luyện thêm: Phát triển ý bằng lý do và ví dụ (Task 2)', 1, 'MULTIPLE_CHOICE',
 'Which sentence best develops the idea "Public transport reduces pollution"?',
 'For example, one bus can replace around 40 cars, which cuts exhaust emissions for each passenger.',
 'Pollution is a bad thing.', 'Many people like buses.', 'A', NULL,
 'A explains how the idea works and gives a concrete example. B and C do not support the claim.',
 'Chọn câu giải thích hoặc minh họa cho ý chính.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-3', 'Luyện thêm: Phát triển ý bằng lý do và ví dụ (Task 2)', 2, 'MULTIPLE_CHOICE',
 'Which body paragraph structure is strongest?',
 'A main idea only', 'A main idea, an explanation and a specific example', 'Three unrelated ideas in one paragraph', 'B', NULL,
 'A developed paragraph states the idea, explains it and supports it with an example. One idea alone is underdeveloped, and three unrelated ideas lack focus.',
 'Ý chính, giải thích, ví dụ.', 'EASY'),
('DEMO_WRITING_CRITERION_TR', 'PS-WCR-TR-3', 'Luyện thêm: Phát triển ý bằng lý do và ví dụ (Task 2)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: support each main idea with a reason and a specific ______.',
 NULL, NULL, NULL, NULL, ARRAY['example', 'illustration'],
 'Specific examples make an argument convincing.',
 'Từ cần điền nghĩa là ví dụ.', 'EASY'),

-- Coherence and Cohesion
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-1', 'Luyện thêm: Chia đoạn rõ ràng', 1, 'MULTIPLE_CHOICE',
 'How many paragraphs does a typical Task 2 essay have?',
 'One long paragraph', 'Four or five clear paragraphs', 'More than ten short paragraphs', 'B', NULL,
 'A typical essay has an introduction, two or three body paragraphs and a conclusion.',
 'Mở bài, thân bài, kết bài.', 'EASY'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-1', 'Luyện thêm: Chia đoạn rõ ràng', 2, 'MULTIPLE_CHOICE',
 'What should each body paragraph focus on?',
 'One central idea', 'All the ideas in the essay', 'Only examples', 'A', NULL,
 'One central idea per paragraph keeps the writing organised and easy to follow.',
 'Mỗi đoạn một ý chính.', 'EASY'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-1', 'Luyện thêm: Chia đoạn rõ ràng', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: start a new ______ when you move to a new main idea.',
 NULL, NULL, NULL, NULL, ARRAY['paragraph'],
 'A new paragraph signals a new main idea.',
 'Đơn vị văn bản lớn hơn một câu.', 'EASY'),

('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-2', 'Luyện thêm: Đại từ tham chiếu và tránh lặp từ', 1, 'MULTIPLE_CHOICE',
 'Choose the best word: "Many students use laptops. ______ allow them to take notes quickly."',
 'They', 'It', 'He', 'A', NULL,
 'Laptops is plural, so the plural pronoun they refers back to it correctly.',
 'Đại từ phải khớp với danh từ số nhiều.', 'EASY'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-2', 'Luyện thêm: Đại từ tham chiếu và tránh lặp từ', 2, 'MULTIPLE_CHOICE',
 'Which version avoids repetition best?',
 'The government should build more parks, which improve health and reduce stress.',
 'The government should build more parks. Parks improve health. Parks reduce stress.',
 'The government should build parks parks parks.', 'A', NULL,
 'A joins the ideas with a relative clause and does not repeat the word parks. B repeats it and C is not meaningful.',
 'Gộp ý bằng mệnh đề quan hệ.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-2', 'Luyện thêm: Đại từ tham chiếu và tránh lặp từ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: in "This trend continues", the word "This" is a ______ word that refers back to an earlier idea.',
 NULL, NULL, NULL, NULL, ARRAY['reference', 'referencing'],
 'Reference words such as this, these and they connect sentences by pointing back to something already mentioned.',
 'Từ này giúp liên kết bằng cách chỉ lại ý đã nêu.', 'MEDIUM'),

('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-3', 'Luyện thêm: Sắp xếp ý và từ nối chuyển đoạn', 1, 'MULTIPLE_CHOICE',
 'Which phrase best introduces a second argument at the start of a new paragraph?',
 'Another important reason is that…', 'In conclusion,', 'For example,', 'A', NULL,
 'Another important reason introduces a new point. In conclusion belongs at the end, and for example introduces support rather than a new argument.',
 'Cần cụm mở ra một lý do mới.', 'EASY'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-3', 'Luyện thêm: Sắp xếp ý và từ nối chuyển đoạn', 2, 'MULTIPLE_CHOICE',
 'Which expression is most suitable at the start of the conclusion?',
 'To sum up,', 'Firstly,', 'For instance,', 'A', NULL,
 'To sum up signals the closing paragraph. Firstly starts a list and for instance gives an example.',
 'Kết bài tóm tắt lại ý.', 'EASY'),
('DEMO_WRITING_CRITERION_CC', 'PS-WCR-CC-3', 'Luyện thêm: Sắp xếp ý và từ nối chuyển đoạn', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: words such as "firstly" and "secondly" show the ______ of your ideas.',
 NULL, NULL, NULL, NULL, ARRAY['order', 'sequence'],
 'Sequencing words tell the reader the order in which ideas are presented.',
 'Từ cần điền nghĩa là thứ tự.', 'EASY'),

-- Lexical Resource
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-1', 'Luyện thêm: Kết hợp từ (collocation)', 1, 'MULTIPLE_CHOICE',
 'Choose the correct collocation: "Governments should ______ action to reduce plastic waste."',
 'make', 'take', 'do', 'B', NULL,
 'The natural collocation is take action. Make action and do action are not used.',
 'Collocation cố định với action.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-1', 'Luyện thêm: Kết hợp từ (collocation)', 2, 'MULTIPLE_CHOICE',
 'Choose the correct verb: "Heavy traffic can ______ serious air pollution."',
 'do', 'make', 'cause', 'C', NULL,
 'Cause is the standard verb for bringing about a negative result. Do and make do not combine with pollution in this sense.',
 'Động từ chỉ nguyên nhân gây ra hậu quả.', 'EASY'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-1', 'Luyện thêm: Kết hợp từ (collocation)', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: leaders must ______ a decision quickly when a crisis begins.',
 NULL, NULL, NULL, NULL, ARRAY['make', 'take'],
 'Make a decision is the usual collocation; take a decision is also used in British English.',
 'Cụm động từ quen thuộc với decision.', 'EASY'),

('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-2', 'Luyện thêm: Diễn đạt lại bằng từ đồng nghĩa', 1, 'MULTIPLE_CHOICE',
 'Which word is the most suitable replacement for "important" in academic writing? "Education is ______ for economic growth."',
 'essential', 'nice', 'big', 'A', NULL,
 'Essential is formal and precise. Nice and big are too informal or too vague.',
 'Chọn từ trang trọng, có nghĩa rất quan trọng.', 'EASY'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-2', 'Luyện thêm: Diễn đạt lại bằng từ đồng nghĩa', 2, 'MULTIPLE_CHOICE',
 'Which sentence paraphrases "More and more people live in cities."?',
 'An increasing number of people reside in urban areas.', 'People are moving to the countryside.',
 'More and more people live in cities.', 'A', NULL,
 'A keeps the meaning with new words. B changes the meaning and C copies the sentence.',
 'Giữ nghĩa nhưng đổi từ và cấu trúc.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-2', 'Luyện thêm: Diễn đạt lại bằng từ đồng nghĩa', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: using different words that keep the same meaning is called ______.',
 NULL, NULL, NULL, NULL, ARRAY['paraphrasing', 'paraphrase'],
 'Paraphrasing shows a range of vocabulary and avoids copying the question.',
 'Từ cần điền nghĩa là diễn đạt lại.', 'EASY'),

('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-3', 'Luyện thêm: Chính tả, dạng từ và văn phong học thuật', 1, 'MULTIPLE_CHOICE',
 'Which word is spelled correctly?',
 'enviroment', 'environment', 'enviornment', 'B', NULL,
 'The correct spelling is environment; spelling errors reduce the Lexical Resource mark.',
 'Chú ý chữ n trước m.', 'EASY'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-3', 'Luyện thêm: Chính tả, dạng từ và văn phong học thuật', 2, 'MULTIPLE_CHOICE',
 'Choose the correct word form: "Rapid ______ has changed city life."',
 'industrialise', 'industrialisation', 'industrial', 'B', NULL,
 'After the adjective rapid a noun is needed: industrialisation. Industrialise is a verb and industrial is an adjective.',
 'Sau tính từ rapid cần một danh từ.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_LR', 'PS-WCR-LR-3', 'Luyện thêm: Chính tả, dạng từ và văn phong học thuật', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: formal writing avoids ______ language such as "kids" or "stuff".',
 NULL, NULL, NULL, NULL, ARRAY['informal', 'colloquial', 'casual', 'slang'],
 'Words like kids and stuff are informal; academic writing prefers children and things, or more precise nouns.',
 'Từ cần điền là trái nghĩa của formal.', 'EASY'),

-- Grammatical Range and Accuracy
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-1', 'Luyện thêm: Câu phức và mệnh đề phụ', 1, 'MULTIPLE_CHOICE',
 'Which sentence is a complex sentence?',
 'Cities grow.', 'Although cities are expensive, many people move there for work.', 'Cities grow and people move.', 'B', NULL,
 'A complex sentence has a main clause and a subordinate clause; B uses although. A is simple and C is compound.',
 'Tìm câu có mệnh đề phụ bắt đầu bằng liên từ phụ thuộc.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-1', 'Luyện thêm: Câu phức và mệnh đề phụ', 2, 'MULTIPLE_CHOICE',
 'Choose the correct relative pronoun: "People ______ live in big cities often face noise."',
 'who', 'which', 'whose', 'A', NULL,
 'Who refers to people as the subject of the clause. Which is used for things and whose shows possession.',
 'Đại từ quan hệ thay cho người, làm chủ ngữ.', 'EASY'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-1', 'Luyện thêm: Câu phức và mệnh đề phụ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: many people move to cities ______ they want better jobs.',
 NULL, NULL, NULL, NULL, ARRAY['because', 'since', 'as'],
 'A subordinate clause of reason is introduced by because, since or as.',
 'Liên từ chỉ nguyên nhân.', 'EASY'),

('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-2', 'Luyện thêm: Thì và hòa hợp chủ vị', 1, 'MULTIPLE_CHOICE',
 'Choose the correct verb: "The number of cars on the road ______ increased dramatically."',
 'have', 'has', 'are', 'B', NULL,
 'The subject is the number (singular), so has is correct. The plural noun cars is not the subject.',
 'Chủ ngữ chính là the number.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-2', 'Luyện thêm: Thì và hòa hợp chủ vị', 2, 'MULTIPLE_CHOICE',
 'Choose the correct tense: "In 2010, the government ______ a new law."',
 'introduces', 'introduced', 'has introduced', 'B', NULL,
 'A finished event with a past date takes the past simple.',
 'Có mốc thời gian quá khứ cụ thể.', 'EASY'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-2', 'Luyện thêm: Thì và hòa hợp chủ vị', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: each of the students ______ expected to submit an essay.',
 NULL, NULL, NULL, NULL, ARRAY['is'],
 'Each is singular, so the verb is is.',
 'Each đi với động từ số ít.', 'MEDIUM'),

('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-3', 'Luyện thêm: Dấu câu và lỗi thường gặp', 1, 'MULTIPLE_CHOICE',
 'Which sentence is punctuated correctly?',
 'Cars cause pollution, however they are convenient.', 'Cars cause pollution; however, they are convenient.',
 'Cars cause pollution however, they are convenient.', 'B', NULL,
 'However joining two independent clauses needs a semicolon before it and a comma after it.',
 'However nối hai mệnh đề độc lập.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-3', 'Luyện thêm: Dấu câu và lỗi thường gặp', 2, 'MULTIPLE_CHOICE',
 'Which sentence contains a comma splice?',
 'Public transport is cheap, it is also reliable.', 'Public transport is cheap and also reliable.',
 'Public transport is cheap; it is also reliable.', 'A', NULL,
 'A joins two independent clauses with only a comma, which is a comma splice. B and C are correct.',
 'Hai mệnh đề độc lập không được nối chỉ bằng dấu phẩy.', 'MEDIUM'),
('DEMO_WRITING_CRITERION_GRA', 'PS-WCR-GRA-3', 'Luyện thêm: Dấu câu và lỗi thường gặp', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: use "fewer" with ______ nouns such as "cars", and "less" with uncountable nouns.',
 NULL, NULL, NULL, NULL, ARRAY['countable', 'plural'],
 'Fewer goes with countable plural nouns; less goes with uncountable nouns.',
 'Danh từ đếm được.', 'MEDIUM');

DO $$
DECLARE
    pkg RECORD;
    item RECORD;
    package_id UUID;
    version_id UUID;
    section_id UUID;
    question_id UUID;
    question_version_id UUID;
    point_id UUID;
BEGIN
    FOR pkg IN SELECT DISTINCT pkg_code, pkg_title, kp_code FROM writing_criterion_items ORDER BY pkg_code LOOP
        SELECT id INTO point_id FROM knowledge_points WHERE code = pkg.kp_code;
        IF point_id IS NULL THEN
            RAISE EXCEPTION 'Knowledge point % is missing for practice set %', pkg.kp_code, pkg.pkg_code;
        END IF;
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

        FOR item IN SELECT * FROM writing_criterion_items WHERE pkg_code = pkg.pkg_code ORDER BY q_no LOOP
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
            VALUES (question_version_id, point_id, 1.0);
            INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
            VALUES (gen_random_uuid(), section_id, question_version_id, item.q_no, 1.0);
        END LOOP;
    END LOOP;

    IF (SELECT count(*) FROM content_packages WHERE code LIKE 'PS-WCR-%') <> 15 THEN
        RAISE EXCEPTION 'Expected three practice sets for each of the five criterion knowledge points';
    END IF;
END $$;
