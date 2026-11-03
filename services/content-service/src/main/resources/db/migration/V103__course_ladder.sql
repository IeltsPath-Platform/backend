-- Courses sit on whole bands: Foundation 4.0 → 5.0 → 6.0 → 7.0 → 8.0. A course teaches the points of its band only;
-- a prerequisite is taught by a lower course or by an earlier chặng of the same course.
--
-- The half bands merge upwards. IELTS 5.5 becomes IELTS 6.0 and IELTS 6.5 becomes IELTS 7.0: those two rows keep their
-- ids, so learners' progress, unlocks and scores stay attached, while the unused, inactive 6.0 and 7.0 rows hand their
-- chặng over and go. The B1 grammar and vocabulary points that sat in the 6.0–7.0 chặng move to the new 5.0 course.

-- 1. Merge the inactive 6.0 / 7.0 rows into the active 5.5 / 6.5 rows, then give those rows the whole band.
UPDATE topics SET course_id = '30000000-0000-4000-8000-000000000001'
WHERE course_id = '30000000-0000-4000-8000-000000000003';
UPDATE topics SET course_id = '30000000-0000-4000-8000-000000000002'
WHERE course_id = '30000000-0000-4000-8000-000000000004';
DELETE FROM courses WHERE id IN ('30000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000004');

UPDATE courses SET code = 'IELTS_6_0', name = 'IELTS 6.0', band_level = 6.0, status = 'ACTIVE',
    description = 'Từ B1 lên B2: nắm các dạng câu hỏi chính của Reading và Listening, viết Task 1 và Task 2 hoàn chỉnh.'
WHERE id = '30000000-0000-4000-8000-000000000001';
UPDATE courses SET code = 'IELTS_7_0', name = 'IELTS 7.0', band_level = 7.0, status = 'ACTIVE',
    description = 'Từ B2 lên C1: suy luận có căn cứ, paraphrase phức tạp, nghe nhiều người nói và viết lập luận chặt chẽ.'
WHERE id = '30000000-0000-4000-8000-000000000002';

UPDATE content_packages SET title = replace(title, 'IELTS 5.5', 'IELTS 6.0')
WHERE course_id = '30000000-0000-4000-8000-000000000001';
UPDATE content_packages SET title = replace(title, 'IELTS 6.5', 'IELTS 7.0')
WHERE course_id = '30000000-0000-4000-8000-000000000002';

-- 2. The new courses. Foundation is free for everyone; its chặng stay within the free lessons of each skill.
INSERT INTO courses (id, code, name, band_level, status, description) VALUES
    ('30000000-0000-4000-8000-000000000005', 'IELTS_FOUNDATION', 'IELTS Foundation', 4.0, 'ACTIVE',
     'Miễn phí cho mọi người. Nền tảng A1–B1: nghe tên và chi tiết quen thuộc, đọc đoạn ngắn, câu cơ bản và đoạn văn đơn giản.'),
    ('30000000-0000-4000-8000-000000000006', 'IELTS_5_0', 'IELTS 5.0', 5.0, 'ACTIVE',
     'Mức B1: ngữ pháp và từ vựng nền cho IELTS, hội thoại hằng ngày, đọc lướt lấy ý và đoạn văn nêu ý kiến.'),
    ('30000000-0000-4000-8000-000000000007', 'IELTS_8_0', 'IELTS 8.0', 8.0, 'ACTIVE',
     'Từ C1 lên C2: hàm ý và giọng điệu, suy luận toàn bài, đọc nhanh, văn phong học thuật và lập luận cân bằng.');

-- 3. Chặng of every course, in teaching order. A course numbers its chặng per skill by this order, so every
-- prerequisite chặng comes first.
UPDATE topics SET course_id = '30000000-0000-4000-8000-000000000005', status = 'ACTIVE',
    name = CASE code WHEN 'FOUNDATION_READING' THEN 'Đọc nền tảng'
                     WHEN 'FOUNDATION_LISTENING' THEN 'Nghe nền tảng'
                     ELSE 'Câu, từ vựng và đoạn văn nền tảng' END,
    sort_order = CASE code WHEN 'FOUNDATION_READING' THEN 100 WHEN 'FOUNDATION_LISTENING' THEN 101 ELSE 102 END
WHERE code IN ('FOUNDATION_READING', 'FOUNDATION_LISTENING', 'FOUNDATION_WRITING');

INSERT INTO topics (id, code, name, sort_order, status, skill, course_id, description) VALUES
    ('31000000-0000-4000-8000-000000000011', 'IELTS_5_0_READING_SKIM', 'Đọc lướt lấy ý đoạn', 200, 'ACTIVE', 'READING', '30000000-0000-4000-8000-000000000006', 'Tìm ý chính của từng đoạn ngắn trước khi đọc kỹ.'),
    ('31000000-0000-4000-8000-000000000012', 'IELTS_5_0_LISTENING_EVERYDAY', 'Hội thoại hằng ngày (Part 1)', 201, 'ACTIVE', 'LISTENING', '30000000-0000-4000-8000-000000000006', 'Nghe đặt chỗ, đăng ký và hỏi thông tin trong tình huống quen thuộc.'),
    ('31000000-0000-4000-8000-000000000013', 'IELTS_5_0_GRAMMAR', 'Ngữ pháp nền cho IELTS', 202, 'ACTIVE', 'WRITING', '30000000-0000-4000-8000-000000000006', 'Mạo từ, hòa hợp chủ ngữ và động từ, so sánh, bị động, từ nối, câu phức và mệnh đề quan hệ.'),
    ('31000000-0000-4000-8000-000000000014', 'IELTS_5_0_READING_SHORT_ANSWER', 'Câu hỏi trả lời ngắn', 203, 'ACTIVE', 'READING', '30000000-0000-4000-8000-000000000006', 'Tìm đúng chi tiết và viết câu trả lời trong giới hạn số từ.'),
    ('31000000-0000-4000-8000-000000000015', 'IELTS_5_0_LISTENING_PREDICT', 'Đoán trước đáp án khi nghe', 204, 'ACTIVE', 'LISTENING', '30000000-0000-4000-8000-000000000006', 'Đọc câu hỏi trước, đoán loại thông tin cần nghe và bắt đúng đáp án.'),
    ('31000000-0000-4000-8000-000000000016', 'IELTS_5_0_VOCABULARY', 'Từ vựng nền cho IELTS', 205, 'ACTIVE', 'WRITING', '30000000-0000-4000-8000-000000000006', 'Từ theo chủ đề, dạng từ, cụm từ học thuật và từ để diễn đạt lại.'),
    ('31000000-0000-4000-8000-000000000017', 'IELTS_5_0_WRITING', 'Câu ghép và đoạn nêu ý kiến', 206, 'ACTIVE', 'WRITING', '30000000-0000-4000-8000-000000000006', 'Nối hai ý thành câu ghép và viết đoạn văn nêu ý kiến có lý do.'),
    ('31000000-0000-4000-8000-000000000021', 'IELTS_8_0_READING_INFERENCE', 'Suy luận ngụ ý toàn bài', 800, 'ACTIVE', 'READING', '30000000-0000-4000-8000-000000000007', 'Nắm lập trường và hàm ý của cả bài, không chỉ của từng câu.'),
    ('31000000-0000-4000-8000-000000000022', 'IELTS_8_0_LISTENING_IMPLIED', 'Hàm ý và giọng điệu người nói', 801, 'ACTIVE', 'LISTENING', '30000000-0000-4000-8000-000000000007', 'Hiểu điều người nói ngụ ý qua cách chọn từ, nhượng bộ và giọng điệu.'),
    ('31000000-0000-4000-8000-000000000023', 'IELTS_8_0_WRITING_STYLE', 'Văn phong học thuật tinh tế', 802, 'ACTIVE', 'WRITING', '30000000-0000-4000-8000-000000000007', 'Chọn từ chính xác, giảm nhẹ khẳng định và giữ giọng văn khách quan.'),
    ('31000000-0000-4000-8000-000000000024', 'IELTS_8_0_READING_TIMED', 'Đọc nhanh dưới áp lực thời gian', 803, 'ACTIVE', 'READING', '30000000-0000-4000-8000-000000000007', 'Phân bổ thời gian và định vị nhanh khi bài dài, nhiều thông tin.'),
    ('31000000-0000-4000-8000-000000000025', 'IELTS_8_0_LISTENING_LECTURE', 'Bài giảng tốc độ cao (Part 4)', 804, 'ACTIVE', 'LISTENING', '30000000-0000-4000-8000-000000000007', 'Theo kịp bài giảng dày thông tin và ghi chú đúng ý chính.'),
    ('31000000-0000-4000-8000-000000000026', 'IELTS_8_0_WRITING_ARGUMENT', 'Lập luận cân bằng và nhượng bộ', 805, 'ACTIVE', 'WRITING', '30000000-0000-4000-8000-000000000007', 'Thừa nhận phản biện, giới hạn lập luận và đi đến kết luận thuyết phục.');

-- IELTS 6.0: scanning is taught before the Reading chặng that build on it, and the opinion essay before the Writing
-- chặng that needs it; the former 6.0 chặng close each skill.
UPDATE topics SET sort_order = CASE code
        WHEN 'DEMO_READING' THEN 600
        WHEN 'TREES_WRITING' THEN 601
        WHEN 'TREES_READING' THEN 602
        WHEN 'DEMO_LISTENING' THEN 603
        WHEN 'DEMO_WRITING' THEN 604
        WHEN 'TFNG_SKILLS' THEN 605
        WHEN 'TREES_LISTENING' THEN 606
        WHEN 'PREMIUM_MATCHING_INFO' THEN 607
        WHEN 'PREMIUM_SENTENCE_COMPLETION' THEN 608
        WHEN 'IELTS_6_0_READING' THEN 609
        WHEN 'IELTS_6_0_LISTENING' THEN 610
        WHEN 'IELTS_6_0_WRITING' THEN 611 END,
    status = 'ACTIVE',
    name = CASE code
        WHEN 'IELTS_6_0_READING' THEN 'Skim cấu trúc và hoàn thành tóm tắt'
        WHEN 'IELTS_6_0_LISTENING' THEN 'Điền form và nhận biết từ dẫn ý'
        WHEN 'IELTS_6_0_WRITING' THEN 'Task 1 đặc điểm chính và liên kết đoạn'
        ELSE name END
WHERE course_id = '30000000-0000-4000-8000-000000000001';

UPDATE topics SET sort_order = CASE code
        WHEN 'READING_6_5_INFERENCE' THEN 700
        WHEN 'LISTENING_6_5_DETAIL' THEN 701
        WHEN 'WRITING_6_5_DISCUSSION' THEN 702
        WHEN 'IELTS_7_0_READING' THEN 703
        WHEN 'IELTS_7_0_LISTENING' THEN 704
        WHEN 'IELTS_7_0_WRITING' THEN 705 END,
    status = 'ACTIVE',
    name = CASE code
        WHEN 'IELTS_7_0_READING' THEN 'Paraphrase phức tạp và quan điểm tác giả'
        WHEN 'IELTS_7_0_LISTENING' THEN 'Bản đồ và thái độ nhiều người nói'
        WHEN 'IELTS_7_0_WRITING' THEN 'Câu phức, phát triển ý và dùng từ chính xác'
        ELSE name END
WHERE course_id = '30000000-0000-4000-8000-000000000002';

-- 4. B1 grammar and vocabulary belong to 5.0. None of them is taught by a lesson or used by a question yet.
UPDATE knowledge_points SET topic_id = '31000000-0000-4000-8000-000000000013'
WHERE code IN ('G55_ARTICLES', 'G55_SUBJECT_VERB_AGREEMENT', 'G55_COMPARISON', 'G55_PASSIVE_VOICE',
               'G55_LINKING_WORDS', 'G55_COMPLEX_SENTENCES', 'G55_RELATIVE_CLAUSES');
UPDATE knowledge_points SET topic_id = '31000000-0000-4000-8000-000000000016'
WHERE code IN ('V55_COMMON_TOPICS', 'V55_WORD_FORMS', 'V55_ACADEMIC_COLLOCATIONS', 'V55_PARAPHRASE_WORDS');

-- 5. Prerequisites. The 6.0 grammar criterion cannot ask for perfect tenses, which 6.0 teaches only in its last Writing
-- chặng. Scanning and the opinion essay are now taught first, so the links that need them hold; a database that lost
-- them gets them back.
DELETE FROM knowledge_point_prerequisites
WHERE knowledge_point_id = (SELECT id FROM knowledge_points WHERE code = 'DEMO_WRITING_CRITERION_GRA')
  AND prerequisite_id = (SELECT id FROM knowledge_points WHERE code = 'G60_PERFECT_TENSES');

INSERT INTO knowledge_point_prerequisites (knowledge_point_id, prerequisite_id)
SELECT dependent.id, prerequisite.id
FROM (VALUES ('PR_MATCHING_INFORMATION', 'TR_R_SCANNING'),
             ('PR_SENTENCE_COMPLETION', 'TR_R_SCANNING'),
             ('DEMO_READING_W2_OPINION', 'TR_W_OPINION')) AS link(dependent_code, prerequisite_code)
JOIN knowledge_points dependent ON dependent.code = link.dependent_code
JOIN knowledge_points prerequisite ON prerequisite.code = link.prerequisite_code
ON CONFLICT DO NOTHING;

-- 6. A milestone is placed after a chặng by its position among the course's chặng; keep each after the same chặng.
UPDATE course_practice_test_milestones milestone
SET after_topic_sequence = ranked.position
FROM (SELECT id, course_id, row_number() OVER (PARTITION BY course_id ORDER BY sort_order, id) AS position
      FROM topics WHERE status = 'ACTIVE' AND course_id IS NOT NULL) ranked
JOIN topics anchor ON anchor.id = ranked.id
WHERE milestone.course_id = ranked.course_id
  AND anchor.code = CASE milestone.course_id
        WHEN '30000000-0000-4000-8000-000000000001' THEN 'DEMO_LISTENING'
        WHEN '30000000-0000-4000-8000-000000000002' THEN 'LISTENING_6_5_DETAIL' END;
