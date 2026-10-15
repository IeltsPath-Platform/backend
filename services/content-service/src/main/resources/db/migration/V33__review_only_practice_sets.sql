-- Review material is kept apart from the practice a lesson requires. A lesson requires every practice set bound to it,
-- and a review must never use one of those sets: it would reveal the answers before the learner practised the set, so
-- the set would no longer count. A PRACTICE_SET without a lesson is review-only material.
--
-- The third and fourth set of each Reading knowledge point leave their lesson (every lesson keeps two required sets),
-- and each Reading knowledge point with a single spare set gets one more, so every one of them has two review sets.

UPDATE content_packages SET lesson_id = NULL
WHERE code IN ('PS-KP1-C', 'PS-KP1-D', 'PS-KP2-C', 'PS-KP3-C', 'PS-KP4-C', 'PS-TF-C');

CREATE TEMP TABLE review_sets_seed (n int, code text, title text, passage text);
CREATE TEMP TABLE review_questions_seed (set_n int, sort_order int, qn int, question_type text, kp uuid, stem text,
                                         options jsonb, correct text, explanation text, hint text);

INSERT INTO review_sets_seed VALUES
    (1, 'PS-KP2-D', 'Luyện thêm PS-KP2-D: Ý chính hay chi tiết (4)', NULL),
    (2, 'PS-KP3-D', 'Luyện thêm PS-KP3-D: Night markets',
     'A. Night markets are a way of life across Asia. Taipei alone has more than a dozen, and some stalls have been run by the same family for decades.

B. Food is the main attraction. Visitors queue for fried chicken, bubble tea and grilled squid, often eating standing up beside the stall.

C. Markets also face pressure. Rents are rising, and city councils want to move stalls to cleaner, more modern buildings.'),
    (3, 'PS-KP4-D', 'Luyện thêm PS-KP4-D: Urban beekeeping',
     'A. Rooftop hives are appearing in cities from London to Tokyo. Beekeepers say city bees often produce more honey than country bees, because parks and gardens flower for most of the year.

B. Bees also help the plants around them. A single hive can pollinate thousands of flowers in a day, which boosts the fruit harvest in nearby allotments.

C. There are limits, however. Too many hives in one district means the bees compete for nectar, and honey yields fall.'),
    (4, 'PS-TF-D', 'Luyện thêm PS-TF-D: False hay Not Given (4)', NULL);

INSERT INTO review_questions_seed VALUES
    -- PS-KP2-D: DR_IDEA_OR_DETAIL
    (1, 1, 1, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000002',
     '"Public libraries are changing to meet new needs." is…',
     '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]',
     'MAIN_IDEA', 'Nhận định chung về cả đoạn; các ví dụ cụ thể về thư viện sẽ triển khai nó.',
     'Câu này bao trùm nhiều ví dụ hay chỉ là một ví dụ?'),
    (1, 2, 2, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000002',
     '"The central library now lends laptops and tablets." is…',
     '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]',
     'DETAIL', 'Đây là một ví dụ cụ thể minh họa cho việc thư viện thay đổi, không phải ý chung.',
     'Câu có nêu một đồ vật hoặc dịch vụ cụ thể không?'),
    (1, 3, 3, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000002',
     '"Reading habits are shifting from print to screens." is…',
     '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]',
     'MAIN_IDEA', 'Xu hướng chung của cả đoạn; số liệu và ví dụ đi kèm chỉ để chứng minh nó.',
     'Câu nói về một xu hướng chung hay một con số cụ thể?'),
    -- PS-KP3-D: DR_TOPIC_SENTENCE
    (2, 1, 4, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000003',
     'Which sentence is the topic sentence of paragraph A?',
     '[{"optionKey": "A", "content": "Taipei alone has more than a dozen.", "sortOrder": 1}, {"optionKey": "B", "content": "Some stalls have been run by the same family for decades.", "sortOrder": 2}, {"optionKey": "C", "content": "Night markets are a way of life across Asia.", "sortOrder": 3}]',
     'C', 'Câu đầu nêu ý chung; Taipei và các gia đình bán hàng chỉ là ví dụ.',
     'Câu chủ đề nêu ý chung; các câu còn lại là ví dụ cho ý đó.'),
    (2, 2, 5, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000003',
     'Which sentence is the topic sentence of paragraph B?',
     '[{"optionKey": "A", "content": "Food is the main attraction.", "sortOrder": 1}, {"optionKey": "B", "content": "Visitors queue for fried chicken, bubble tea and grilled squid.", "sortOrder": 2}, {"optionKey": "C", "content": "Visitors often eat standing up beside the stall.", "sortOrder": 3}]',
     'A', '"Food is the main attraction" bao cả các món được kể phía sau.',
     'Câu nào bao được các món ăn còn lại?'),
    (2, 3, 6, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000003',
     'Which sentence is the topic sentence of paragraph C?',
     '[{"optionKey": "A", "content": "Markets also face pressure.", "sortOrder": 1}, {"optionKey": "B", "content": "Rents are rising.", "sortOrder": 2}, {"optionKey": "C", "content": "City councils want to move stalls to modern buildings.", "sortOrder": 3}]',
     'A', 'Câu đầu nêu ý chung (áp lực lên chợ); tiền thuê tăng và kế hoạch di dời là hai ví dụ.',
     'Hai áp lực nào được nêu? Câu nào gọi tên chung cho cả hai?'),
    -- PS-KP4-D: DR_MATCHING_HEADINGS
    (3, 1, 7, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000004',
     'Choose the best heading for paragraph A.',
     '[{"optionKey": "i", "content": "Why city bees do well", "sortOrder": 1}, {"optionKey": "ii", "content": "A threat to nearby gardens", "sortOrder": 2}, {"optionKey": "iii", "content": "The history of honey", "sortOrder": 3}, {"optionKey": "iv", "content": "Rules for keeping bees", "sortOrder": 4}]',
     'i', 'Đoạn A giải thích vì sao ong thành phố cho nhiều mật hơn (công viên, vườn nở hoa quanh năm).',
     'Heading phải khớp ý chính của cả đoạn, không chỉ một chi tiết.'),
    (3, 2, 8, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000004',
     'Choose the best heading for paragraph B.',
     '[{"optionKey": "i", "content": "Bees that help gardens grow", "sortOrder": 1}, {"optionKey": "ii", "content": "A shortage of nectar", "sortOrder": 2}, {"optionKey": "iii", "content": "Building a rooftop hive", "sortOrder": 3}, {"optionKey": "iv", "content": "Buying honey online", "sortOrder": 4}]',
     'i', 'Đoạn B nói ong thụ phấn giúp cây và vụ trái cây quanh đó.',
     'Đoạn B nói ong mang lại lợi ích gì cho cây?'),
    (3, 3, 9, 'MULTIPLE_CHOICE', '20000000-0000-4000-8000-020000000004',
     'Choose the best heading for paragraph C.',
     '[{"optionKey": "i", "content": "The cost of beekeeping equipment", "sortOrder": 1}, {"optionKey": "ii", "content": "When there are too many hives", "sortOrder": 2}, {"optionKey": "iii", "content": "Pollination in the countryside", "sortOrder": 3}, {"optionKey": "iv", "content": "A new kind of bee", "sortOrder": 4}]',
     'ii', 'Đoạn C nêu giới hạn: quá nhiều tổ ong trong một khu thì ong tranh mật và sản lượng giảm.',
     'Từ "however" báo trước điều gì ở đoạn này?'),
    -- PS-TF-D: TFNG_FALSE_VS_NOT_GIVEN
    (4, 1, 10, 'TRUE_FALSE_NOT_GIVEN', '20000000-0000-4000-8000-020000000005',
     'Passage: "The bridge opened in 1932 and carries about 40,000 vehicles a day." Statement: "The bridge is used by thousands of vehicles every day."',
     '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]',
     'TRUE', '40,000 xe mỗi ngày là hàng chục nghìn, nên "thousands of vehicles" đúng: TRUE.',
     '40,000 có thuộc nhóm "thousands" không?'),
    (4, 2, 11, 'TRUE_FALSE_NOT_GIVEN', '20000000-0000-4000-8000-020000000005',
     'Passage: "The festival takes place every July in the old town." Statement: "The festival attracts visitors from abroad."',
     '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]',
     'NOT_GIVEN', 'Đoạn văn chỉ nói thời gian và địa điểm, không nói gì về khách nước ngoài: NOT GIVEN.',
     'Đoạn văn có nhắc tới khách nước ngoài không?'),
    (4, 3, 12, 'TRUE_FALSE_NOT_GIVEN', '20000000-0000-4000-8000-020000000005',
     'Passage: "All students must hand in the form by Friday." Statement: "Students can hand in the form after the weekend."',
     '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]',
     'FALSE', '"Must ... by Friday" mâu thuẫn với việc nộp sau cuối tuần: FALSE.',
     'Hạn nộp là khi nào? Sau cuối tuần có còn trong hạn không?');

-- Ids follow the seed pattern: a prefix, a two-digit kind and a counter.
CREATE FUNCTION pg_temp.seed_id(kind text, n int) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT ('32000000-0000-4000-8000-' || kind || lpad(n::text, 10, '0'))::uuid $$;

INSERT INTO content_assets (id, asset_type, text_content, validation_status)
SELECT pg_temp.seed_id('05', n), 'PASSAGE', passage, 'VALID' FROM review_sets_seed WHERE passage IS NOT NULL;

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key)
SELECT pg_temp.seed_id('08', n), code, title, 'PRACTICE_SET', NULL, NULL, 'PUBLISHED', NULL FROM review_sets_seed;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
SELECT pg_temp.seed_id('09', n), pg_temp.seed_id('08', n), 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP
FROM review_sets_seed;

UPDATE content_packages p SET current_published_version_id = pg_temp.seed_id('09', s.n)
FROM review_sets_seed s WHERE p.id = pg_temp.seed_id('08', s.n);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
SELECT pg_temp.seed_id('0a', n), pg_temp.seed_id('09', n), title, 'READING', 1 FROM review_sets_seed;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
SELECT pg_temp.seed_id('0c', n), pg_temp.seed_id('05', n), pg_temp.seed_id('0a', n), 0
FROM review_sets_seed WHERE passage IS NOT NULL;

INSERT INTO questions (id, question_type, skill, status)
SELECT pg_temp.seed_id('06', qn), question_type, 'READING', 'PUBLISHED' FROM review_questions_seed;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version,
                               explanation, hint, status)
SELECT pg_temp.seed_id('07', qn), pg_temp.seed_id('06', qn), 1, stem, options,
       jsonb_build_object('type', 'CHOICE', 'correct', correct), 1, explanation, hint, 'PUBLISHED'
FROM review_questions_seed;

UPDATE questions q SET current_published_version_id = pg_temp.seed_id('07', s.qn)
FROM review_questions_seed s WHERE q.id = pg_temp.seed_id('06', s.qn);

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT pg_temp.seed_id('07', qn), kp, 1.00 FROM review_questions_seed;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
SELECT pg_temp.seed_id('0b', qn), pg_temp.seed_id('0a', set_n), pg_temp.seed_id('07', qn), sort_order, 1.00
FROM review_questions_seed;

DROP TABLE review_questions_seed;
DROP TABLE review_sets_seed;
