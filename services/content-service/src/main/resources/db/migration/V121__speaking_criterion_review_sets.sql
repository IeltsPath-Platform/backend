-- The four criterion knowledge points of the Speaking rubric (FC, LR, GRA, PR) take evidence again: a recorded answer in a
-- lesson's practice set is judged on each criterion, and a criterion below the pass band earns an optional review, as a
-- weak Writing criterion does. A learner who is weak in one criterion needs sets to review, so each point gets three
-- free-standing practice sets of three questions. The sets belong to no lesson or topic, so they gate nothing.
-- This is AI-authored demo content: it requires review by an IELTS-qualified examiner before it is shown to learners as
-- definitive teaching material.

UPDATE knowledge_points
   SET status = 'ACTIVE', cefr_level = COALESCE(cefr_level, 'B2'), updated_at = now(),
       can_do = COALESCE(NULLIF(btrim(can_do), ''), CASE code
           WHEN 'DEMO_SPEAKING_CRITERION_FC' THEN 'Nói liên tục, nối các ý bằng từ nối đơn giản và không dừng quá lâu.'
           WHEN 'DEMO_SPEAKING_CRITERION_LR' THEN 'Chọn từ chính xác và diễn đạt lại bằng từ đơn giản khi quên từ.'
           WHEN 'DEMO_SPEAKING_CRITERION_GRA' THEN 'Nói câu dài với because, although, if và giữ đúng thì động từ.'
           ELSE 'Nói rõ từng từ với trọng âm, nhịp và ngữ điệu dễ nghe.' END)
 WHERE code IN ('DEMO_SPEAKING_CRITERION_FC', 'DEMO_SPEAKING_CRITERION_LR', 'DEMO_SPEAKING_CRITERION_GRA',
                'DEMO_SPEAKING_CRITERION_PR');

CREATE TEMP TABLE speaking_criterion_items (
    kp_code TEXT, pkg_code TEXT, pkg_title TEXT, q_no INT, qtype TEXT, stem TEXT,
    opt_a TEXT, opt_b TEXT, opt_c TEXT, correct TEXT, accepted TEXT[], explanation TEXT, hint TEXT, difficulty TEXT
) ON COMMIT DROP;

INSERT INTO speaking_criterion_items VALUES
-- Fluency and coherence
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-1', 'Luyện thêm: Nói liên tục khi cần suy nghĩ', 1, 'MULTIPLE_CHOICE',
 'You cannot think of the next idea while you are speaking. What is the best thing to do?',
 'Stay silent until the idea comes', 'Say a short phrase such as "Well, let me think" and keep talking', 'Switch to Vietnamese', 'B', NULL,
 'A short filler phrase keeps the answer moving. Long silence breaks the flow and Vietnamese is not allowed in the test.',
 'Dùng cụm đệm ngắn để có thời gian nghĩ.', 'EASY'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-1', 'Luyện thêm: Nói liên tục khi cần suy nghĩ', 2, 'MULTIPLE_CHOICE',
 'Which phrase is a natural way to gain thinking time?',
 'Um um um um um', '"That is an interesting question."', '"I do not know."', 'B', NULL,
 'B sounds natural and moves the answer on. Repeating "um" is hesitation, and C ends the answer.',
 'Chọn câu nghe tự nhiên mà vẫn nói tiếp được.', 'EASY'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-1', 'Luyện thêm: Nói liên tục khi cần suy nghĩ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: fluent speech keeps a steady ______ without long pauses.',
 NULL, NULL, NULL, NULL, ARRAY['pace', 'flow', 'rhythm', 'speed'],
 'A steady pace or flow is what the examiner hears as fluency.',
 'Từ cần điền nói về tốc độ hoặc dòng chảy của lời nói.', 'EASY'),

('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-2', 'Luyện thêm: Nối ý bằng từ nối đơn giản', 1, 'MULTIPLE_CHOICE',
 'Which word adds a reason? "I like my town ______ it is quiet."',
 'but', 'because', 'although', 'B', NULL,
 'Because introduces a reason. But and although show contrast.',
 'Từ nối chỉ nguyên nhân.', 'EASY'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-2', 'Luyện thêm: Nối ý bằng từ nối đơn giản', 2, 'MULTIPLE_CHOICE',
 'Choose the best linker: "I like cities, ______ I prefer the countryside for holidays."',
 'because', 'but', 'so', 'B', NULL,
 'The two ideas contrast, so but fits. Because gives a reason and so gives a result.',
 'Hai ý đối lập nhau.', 'EASY'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-2', 'Luyện thêm: Nối ý bằng từ nối đơn giản', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "It was raining, ______ we stayed at home." (a result)',
 NULL, NULL, NULL, NULL, ARRAY['so'],
 'So introduces a result of the first idea.',
 'Từ nối chỉ kết quả.', 'EASY'),

('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-3', 'Luyện thêm: Sắp xếp ý rõ ràng', 1, 'MULTIPLE_CHOICE',
 'Which answer to "Do you like cooking?" is best organised?',
 'A direct answer, then a reason, then a short example', 'Three examples and no answer', 'A long story about a different topic', 'A', NULL,
 'An answer first, then support, is easy for the listener to follow.',
 'Trả lời trước, giải thích sau.', 'EASY'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-3', 'Luyện thêm: Sắp xếp ý rõ ràng', 2, 'MULTIPLE_CHOICE',
 'What is the best use of the one minute of preparation time in Part 2?',
 'Write a full script', 'Note key words for each part of the task', 'Wait without writing', 'B', NULL,
 'Key words remind you of each point without making you read a script.',
 'Ghi từ khóa, không viết cả bài.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_FC', 'PS-SCR-FC-3', 'Luyện thêm: Sắp xếp ý rõ ràng', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: start with a direct ______ to the question, then add a reason.',
 NULL, NULL, NULL, NULL, ARRAY['answer', 'response', 'reply'],
 'Answering the question first shows the listener your main point.',
 'Điều đầu tiên cần làm với câu hỏi.', 'EASY'),

-- Lexical resource
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-1', 'Luyện thêm: Dùng từ chính xác', 1, 'MULTIPLE_CHOICE',
 'Which word is more precise than "very big"?',
 'enormous', 'big big', 'more big', 'A', NULL,
 'Enormous says the size exactly. The other two are not natural English.',
 'Chọn một từ thay cho very + tính từ.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-1', 'Luyện thêm: Dùng từ chính xác', 2, 'MULTIPLE_CHOICE',
 'Which sentence is the most precise?',
 'The food was good.', 'The food was delicious.', 'The food was good good.', 'B', NULL,
 'Delicious tells the listener more than good. Repeating a word is not a way to add meaning.',
 'Từ cụ thể hơn cho thấy vốn từ rộng hơn.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-1', 'Luyện thêm: Dùng từ chính xác', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD that replaces "very tired": "After the trip I was ______."',
 NULL, NULL, NULL, NULL, ARRAY['exhausted', 'worn', 'drained', 'shattered'],
 'Exhausted means very tired in a single word.',
 'Một tính từ mạnh hơn tired.', 'MEDIUM'),

('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-2', 'Luyện thêm: Diễn đạt lại khi quên từ', 1, 'MULTIPLE_CHOICE',
 'You forget the word "pharmacy". What should you do?',
 'Stop speaking', 'Say "the shop where you buy medicine"', 'Say the word in Vietnamese', 'B', NULL,
 'Describing the word in simpler English keeps you speaking and shows flexible vocabulary.',
 'Mô tả từ bằng từ đơn giản hơn.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-2', 'Luyện thêm: Diễn đạt lại khi quên từ', 2, 'MULTIPLE_CHOICE',
 'Which phrase shows that you are explaining a word?',
 '"What I mean is..."', '"Never mind."', '"Skip this."', 'A', NULL,
 '"What I mean is" introduces a simpler explanation of the idea.',
 'Cụm báo hiệu bạn sắp giải thích.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-2', 'Luyện thêm: Diễn đạt lại khi quên từ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: when you forget a word, ______ it in simpler words.',
 NULL, NULL, NULL, NULL, ARRAY['describe', 'explain', 'paraphrase', 'rephrase'],
 'Describing or paraphrasing the missing word keeps the answer going.',
 'Động từ nghĩa là giải thích bằng từ khác.', 'EASY'),

('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-3', 'Luyện thêm: Cụm từ đi cùng nhau', 1, 'MULTIPLE_CHOICE',
 'Which phrase is natural?',
 'make homework', 'do homework', 'create homework', 'B', NULL,
 'English says "do homework". Words go together in fixed ways called collocations.',
 'Nhớ cụm đi cùng nhau, đừng dịch từng từ.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-3', 'Luyện thêm: Cụm từ đi cùng nhau', 2, 'MULTIPLE_CHOICE',
 'Choose the natural verb: "I like to ______ photos when I travel."',
 'take', 'make', 'do', 'A', NULL,
 'We take photos. "Make a photo" is not used in English.',
 'Động từ đi với photos.', 'EASY'),
('DEMO_SPEAKING_CRITERION_LR', 'PS-SCR-LR-3', 'Luyện thêm: Cụm từ đi cùng nhau', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "I usually ______ a decision after thinking carefully."',
 NULL, NULL, NULL, NULL, ARRAY['make', 'take', 'reach'],
 'Make a decision is the usual collocation.',
 'Động từ đi với decision.', 'MEDIUM'),

-- Grammatical range and accuracy
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-1', 'Luyện thêm: Câu dài với because, although, if', 1, 'MULTIPLE_CHOICE',
 'Which sentence uses a longer, joined structure?',
 'I like it.', 'I like it because it is quiet.', 'Like it.', 'B', NULL,
 'B joins two ideas with because. A is a short simple sentence and C is not a full sentence.',
 'Nối hai ý thành một câu.', 'EASY'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-1', 'Luyện thêm: Câu dài với because, although, if', 2, 'MULTIPLE_CHOICE',
 'Choose the correct word: "______ it was raining, we went out."',
 'Because', 'Although', 'So', 'B', NULL,
 'Although shows contrast: going out was not what rain suggests.',
 'Hai ý đối lập nhau.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-1', 'Luyện thêm: Câu dài với because, although, if', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "If I ______ more time, I would travel more."',
 NULL, NULL, NULL, NULL, ARRAY['had'],
 'An unreal present condition uses the past form: if I had.',
 'Câu điều kiện không có thật ở hiện tại.', 'MEDIUM'),

('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-2', 'Luyện thêm: Dùng đúng thì khi nói', 1, 'MULTIPLE_CHOICE',
 'You talk about last weekend. Which sentence is correct?',
 'I go to the park.', 'I went to the park.', 'I will went to the park.', 'B', NULL,
 'A finished past event needs the past simple.',
 'Việc đã xảy ra xong cần thì quá khứ đơn.', 'EASY'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-2', 'Luyện thêm: Dùng đúng thì khi nói', 2, 'MULTIPLE_CHOICE',
 'Choose the correct sentence.',
 'I live in this city since 2015.', 'I lived in this city since 2015.', 'I have lived in this city since 2015.', 'C', NULL,
 'Since with a situation that continues to now takes the present perfect.',
 'Since + mốc thời gian dùng với hiện tại hoàn thành.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-2', 'Luyện thêm: Dùng đúng thì khi nói', 3, 'FILL_IN_BLANK',
 'Complete with the correct form of "watch": "Yesterday I ______ a film with my friends."',
 NULL, NULL, NULL, NULL, ARRAY['watched'],
 'The word yesterday needs the past simple: watched.',
 'Dạng quá khứ của watch.', 'EASY'),

('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-3', 'Luyện thêm: Chủ ngữ - động từ và câu hoàn chỉnh', 1, 'MULTIPLE_CHOICE',
 'Which sentence is correct?',
 'My brother like football.', 'My brother likes football.', 'My brother liking football.', 'B', NULL,
 'A third person singular subject takes -s in the present simple.',
 'Chủ ngữ ngôi thứ ba số ít thêm -s.', 'EASY'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-3', 'Luyện thêm: Chủ ngữ - động từ và câu hoàn chỉnh', 2, 'MULTIPLE_CHOICE',
 'Which is a complete sentence?',
 'Because I was tired.', 'I stayed at home because I was tired.', 'Staying at home.', 'B', NULL,
 'B has a main clause. A and C are fragments that cannot stand alone.',
 'Câu hoàn chỉnh cần mệnh đề chính.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_GRA', 'PS-SCR-GRA-3', 'Luyện thêm: Chủ ngữ - động từ và câu hoàn chỉnh', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: "There ______ many people at the market."',
 NULL, NULL, NULL, NULL, ARRAY['are'],
 'Many people is plural, so the verb is are.',
 'Danh từ số nhiều.', 'EASY'),

-- Pronunciation
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-1', 'Luyện thêm: Trọng âm của từ', 1, 'MULTIPLE_CHOICE',
 'Where is the main stress in the noun "photograph"?',
 'The first syllable: PHO-to-graph', 'The second syllable: pho-TO-graph', 'The last syllable: pho-to-GRAPH', 'A', NULL,
 'The noun photograph is stressed on the first syllable.',
 'Nghe âm tiết nào to và dài nhất.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-1', 'Luyện thêm: Trọng âm của từ', 2, 'MULTIPLE_CHOICE',
 'Where is the main stress in "important"?',
 'IM-por-tant', 'im-POR-tant', 'im-por-TANT', 'B', NULL,
 'Important is stressed on the second syllable.',
 'Trọng âm rơi vào âm tiết thứ hai.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-1', 'Luyện thêm: Trọng âm của từ', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: the part of a word that is said louder and longer is the stressed ______.',
 NULL, NULL, NULL, NULL, ARRAY['syllable'],
 'Each word is made of syllables, and one of them carries the stress.',
 'Đơn vị âm thanh của từ.', 'EASY'),

('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-2', 'Luyện thêm: Nhịp và ngữ điệu', 1, 'MULTIPLE_CHOICE',
 'A yes/no question such as "Are you ready?" usually ends with:',
 'Rising intonation', 'Falling intonation', 'A flat voice', 'A', NULL,
 'A yes/no question normally rises at the end.',
 'Giọng lên ở cuối câu hỏi có/không.', 'EASY'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-2', 'Luyện thêm: Nhịp và ngữ điệu', 2, 'MULTIPLE_CHOICE',
 'Which habit gives English a natural rhythm?',
 'Say every word with equal strength', 'Stress the important words and say small words quickly', 'Speak as fast as possible', 'B', NULL,
 'English stresses content words and reduces small words such as of and to.',
 'Nhấn từ quan trọng, lướt nhanh từ nhỏ.', 'MEDIUM'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-2', 'Luyện thêm: Nhịp và ngữ điệu', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: intonation is how your voice goes up and ______.',
 NULL, NULL, NULL, NULL, ARRAY['down', 'falls', 'fall'],
 'Intonation is the rise and fall of the voice.',
 'Ngược nghĩa với up.', 'EASY'),

('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-3', 'Luyện thêm: Âm cuối và từ khó', 1, 'MULTIPLE_CHOICE',
 'Why do final sounds matter? Think of "walk" and "wall".',
 'A missing final sound can change the word', 'They never matter', 'They matter only in writing', 'A', NULL,
 'Dropping a final sound can make the listener hear a different word.',
 'Thử nói hai từ walk và wall.', 'EASY'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-3', 'Luyện thêm: Âm cuối và từ khó', 2, 'MULTIPLE_CHOICE',
 'The feedback marks a word as mispronounced. What is the best next step?',
 'Ignore it', 'Listen to the word, repeat it slowly, then use it in a sentence', 'Avoid the word forever', 'B', NULL,
 'Slow repetition, then use in a sentence, moves the sound into natural speech.',
 'Nghe, nhắc lại chậm, rồi đặt vào câu.', 'EASY'),
('DEMO_SPEAKING_CRITERION_PR', 'PS-SCR-PR-3', 'Luyện thêm: Âm cuối và từ khó', 3, 'FILL_IN_BLANK',
 'Complete with ONE WORD: say a difficult word slowly first, then in a full ______.',
 NULL, NULL, NULL, NULL, ARRAY['sentence'],
 'A word in a sentence is practised the way you will really use it.',
 'Đơn vị lớn hơn một từ.', 'EASY');

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
    FOR pkg IN SELECT DISTINCT pkg_code, pkg_title, kp_code FROM speaking_criterion_items ORDER BY pkg_code LOOP
        CONTINUE WHEN EXISTS (SELECT 1 FROM content_packages WHERE code = pkg.pkg_code);
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
        VALUES (section_id, version_id, pkg.pkg_title, 'SPEAKING', 1);

        FOR item IN SELECT * FROM speaking_criterion_items WHERE pkg_code = pkg.pkg_code ORDER BY q_no LOOP
            question_id := gen_random_uuid();
            question_version_id := gen_random_uuid();
            INSERT INTO questions (id, question_type, skill, purpose, status)
            VALUES (question_id, item.qtype, 'SPEAKING', 'LEARNING', 'PUBLISHED');
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

    IF (SELECT count(*) FROM content_packages WHERE code LIKE 'PS-SCR-%') <> 12 THEN
        RAISE EXCEPTION 'Expected three practice sets for each of the four Speaking criterion knowledge points';
    END IF;
END $$;
