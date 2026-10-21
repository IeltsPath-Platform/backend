DO $$
DECLARE
    task RECORD;
    writing_topic CONSTANT UUID := '25000000-0000-4000-8000-010000000001';
    writing_lesson CONSTANT UUID := '25000000-0000-4000-8000-030000000001';
    package_id UUID;
    version_id UUID;
    section_id UUID;
    question_id UUID;
    question_version_id UUID;
    chart_asset_id UUID;
    chart_url CONSTANT TEXT := 'https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writtings/leassons/PS-W1-WR-3/ps-w1-wr-3-marriage-age.png';
    chart_alt CONSTANT TEXT := 'Situation of marriage and average age at marriage in Australia, 1960 to 2000. Religious marriages: 25,000, 28,000, 50,000, 32,000, 34,000; civil marriages: 8,000, 12,000, 16,000, 23,000, 40,000; average bride ages: 21, 23, 25, 27, 30; average groom ages: 24, 25, 27, 30, 32.';
BEGIN
    FOR task IN SELECT * FROM (VALUES (
        'PS-W1-WR-3',
        'Viết bài: Bảng số liệu (hôn nhân và tuổi kết hôn)',
        'DEMO_READING_W1_CHART',
        'The table below gives information about the situation of marriage and age from 1960 to 2000 in Australia. Religious marriage: 25,000 in 1960, 28,000 in 1970, 50,000 in 1980, 32,000 in 1990 and 34,000 in 2000. Civil marriage: 8,000, 12,000, 16,000, 23,000 and 40,000. Average age of bride: 21, 23, 25, 27 and 30. Average age of groom: 24, 25, 27, 30 and 32. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.',
        '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5,"chartFacts":"Marriages in Australia by type (number of marriages) and average age at marriage, 1960 to 2000. Religious marriage: 25,000 (1960), 28,000 (1970), 50,000 (1980), 32,000 (1990), 34,000 (2000). Civil marriage: 8,000, 12,000, 16,000, 23,000, 40,000. Average age of bride: 21, 23, 25, 27, 30. Average age of groom: 24, 25, 27, 30, 32. Key features: both marriage types rose overall; religious marriage peaked at 50,000 in 1980, fell to 32,000 in 1990 and recovered slightly to 34,000; civil marriage rose in every period and overtook religious marriage in 2000 (40,000 against 34,000); the average age of both brides and grooms rose every decade; grooms were older than brides in every year (gap of 3 years in 1960 and 1990, 2 years in 1970, 1980 and 2000)."}',
        '## 1. Phân tích đề và viết Introduction
Bảng gồm hai nhóm dữ liệu tại Australia từ năm 1960 đến 2000: số cuộc hôn nhân theo tôn giáo/dân sự và tuổi kết hôn trung bình của cô dâu/chú rể. Các con số hôn nhân là số cuộc; tuổi tính bằng năm.
- Có thể diễn đạt lại “gives information about the situation of marriage and age” thành “compares marriage numbers by type and the average ages of brides and grooms”.
- Khung câu: The table compares the numbers of religious and civil marriages in Australia with the average ages of brides and grooms between 1960 and 2000.
## 2. Idea cho Overview
Bảng chia làm **2 nhóm thông tin rõ rệt**: (1) số lượng hai loại hôn nhân, (2) độ tuổi kết hôn trung bình. Chọn 2 main features theo đúng hai nhóm này, kết hợp cả **trend** lẫn **difference** để overview không bị đơn điệu.
- **Trend:** religious marriage và civil marriage nhìn chung đi lên trong suốt giai đoạn 1960–2000; tuổi kết hôn trung bình của cả cô dâu và chú rể cũng tăng dần qua từng thập kỷ.
- **Difference:** religious marriage phổ biến hơn civil marriage trong phần lớn giai đoạn, nhưng đến năm 2000 civil marriage vượt lên; đồng thời nam luôn kết hôn ở độ tuổi lớn hơn nữ trong suốt cả giai đoạn.
Overview chỉ nêu ý chính của hai nhóm, không dẫn số liệu chi tiết.
## 3. Chia nhóm cho hai đoạn thân bài
Phân body theo **nhóm dữ liệu** chứ không theo mốc thời gian, vì bảng có hai loại thông tin tách biệt (loại hôn nhân và độ tuổi kết hôn). Tách riêng như vậy dễ so sánh trong từng nhóm và tránh lặp ý giữa hai body.
- **Body 1:** gom religious marriage và civil marriage, trình bày diễn biến số lượng của từng loại và điểm đảo chiều ở năm 2000.
- **Body 2:** gom average age of bride và average age of groom, trình bày xu hướng tăng dần và mức chênh lệch tuổi giữa hai giới.
## 4. Triển khai từng câu
- **Body 1, câu 1:** Religious marriage khởi đầu vượt trội hơn hẳn civil marriage (25,000 so với 8,000 vào năm 1960).
- **Body 1, câu 2:** Con số này tăng gấp đôi, đạt đỉnh 50,000 vào năm 1980, sau đó giảm xuống 32,000 (1990), rồi phục hồi nhẹ lên 34,000 (2000).
- **Body 1, câu 3:** Trong khi đó, civil marriage tăng đều hơn nhưng rõ rệt về sau: đạt 23,000 vào năm 1990, rồi tăng khoảng 74% lên 40,000 vào năm 2000, vượt qua religious marriage.
- **Body 2, câu 1:** Xuyên suốt cả giai đoạn, cô dâu ở Úc luôn trẻ hơn chú rể.
- **Body 2, câu 2:** Năm 1960, tuổi kết hôn trung bình của cô dâu là 21, trẻ hơn chú rể (24 tuổi) 3 tuổi.
- **Body 2, câu 3:** Cả hai chỉ số đều tăng qua từng thập kỷ (mỗi lần tăng 1–3 tuổi); đến năm 2000 đạt 30 (cô dâu) và 32 (chú rể), tức kết hôn muộn hơn khoảng 8–9 tuổi so với năm 1960.
## 5. Ngôn ngữ và tự kiểm tra
- Dùng thì quá khứ khi mô tả các mốc đã qua; kiểm tra thứ tự bride/groom khi dùng respectively.
- Viết dấu phẩy trong số hàng nghìn (25,000); kiểm tra đủ Introduction, Overview, Body 1, Body 2 và tối thiểu 150 từ.
## 6. Paraphrasing
Bài này cần đa dạng cách viết “tăng/giảm” khi miêu tả số lượng hôn nhân và độ tuổi kết hôn.
- X''s figure doubled and peaked at [số liệu]
- X''s figure fell / (slightly) recovered to [số liệu]
- there was a rise in the figures for X, which reached [số liệu]
- followed by a sharp rise to [số liệu]
- X rose steadily, increasing by about [số] each decade
## 7. Từ vựng cần nhớ
- **respective figures of X and Y**: các con số tương ứng lần lượt của X và Y || dẫn hai số liệu cùng lúc mà không lặp cấu trúc
- **overtake**: vượt qua || dùng khi một số liệu vượt lên trên số liệu còn lại (civil marriages overtook religious marriages)
- **twofold increase**: mức tăng gấp đôi || danh từ thay cho “double”, chỉ dùng khi thật sự gấp đôi (religious marriage 25,000 lên 50,000)
- **steadily**: một cách đều đặn || dùng cho xu hướng ổn định, không đột biến, khác với “dramatically” (mạnh, đột ngột)
- **counterpart**: đối tượng tương ứng (bride // groom) || tránh lặp từ khi so sánh hai nhóm đối lập trong cùng câu',
        'Gợi ý bài mẫu: mở bài diễn đạt lại bảng; overview nêu xu hướng tăng chung và hai điểm khác biệt lớn (civil vượt religious năm 2000; chú rể luôn lớn tuổi hơn cô dâu). Body 1 mô tả hai loại hôn nhân, Body 2 mô tả tuổi kết hôn.',
        'Phân body theo nhóm dữ liệu, không theo mốc thời gian.'
    )) AS tasks(pkg_code, pkg_title, kp_code, stem, answer_spec, guide, explanation, hint) LOOP
        package_id := gen_random_uuid();
        version_id := gen_random_uuid();
        section_id := gen_random_uuid();
        question_id := gen_random_uuid();
        question_version_id := gen_random_uuid();
        INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status)
        VALUES (package_id, task.pkg_code, task.pkg_title, 'PRACTICE_SET', writing_topic, writing_lesson, 'PUBLISHED');
        INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
        VALUES (version_id, package_id, 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);
        UPDATE content_packages SET current_published_version_id = version_id WHERE id = package_id;
        INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
        VALUES (section_id, version_id, 'Bài viết', 'WRITING', 1);
        INSERT INTO questions (id, question_type, skill, purpose, status)
        VALUES (question_id, 'ESSAY', 'WRITING', 'LEARNING', 'PUBLISHED');
        INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version,
                                       explanation, hint, difficulty, status, guide)
        VALUES (question_version_id, question_id, 1, task.stem, NULL, task.answer_spec::jsonb, 1,
                task.explanation, task.hint, 'MEDIUM', 'PUBLISHED', task.guide);
        UPDATE questions SET current_published_version_id = question_version_id WHERE id = question_id;
        INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
        VALUES (question_version_id, (SELECT id FROM knowledge_points WHERE code = task.kp_code), 1.0);
        INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
        VALUES (gen_random_uuid(), section_id, question_version_id, 1, 1.0);

        chart_asset_id := md5('course-writing-chart:' || task.pkg_code)::uuid;
        INSERT INTO content_assets (id, asset_type, text_content, media_reference, validation_status)
        VALUES (chart_asset_id, 'IMAGE', chart_alt, chart_url, 'VALID')
        ON CONFLICT (id) DO UPDATE
        SET text_content = EXCLUDED.text_content, media_reference = EXCLUDED.media_reference,
            validation_status = EXCLUDED.validation_status;
        INSERT INTO content_asset_links (id, asset_id, question_version_id, sort_order)
        VALUES (md5('course-writing-chart-link:' || question_version_id::text)::uuid,
                chart_asset_id, question_version_id, 0)
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;
