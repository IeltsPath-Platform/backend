-- DRAFT knowledge tree for bands 6.0 and 7.0, pending review by an IELTS expert.
-- Old knowledge points keep their code and topic; their displayed band follows the course that holds their topic.
-- An edge "A -> B" means A is a prerequisite of B and is stored as (knowledge_point_id = B, prerequisite_id = A).
-- Edges follow real difficulty, not the displayed band.

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('31000000-0000-4000-8000-020000000001', '31000000-0000-4000-8000-000000000001', 'R60_SKIM_STRUCTURE', 'Skim cấu trúc bài đọc', 'STRATEGY', 'PROCEDURE', 'READING', 'Đọc lướt để nắm cấu trúc và ý chính của từng đoạn trước khi làm câu hỏi.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000002', '31000000-0000-4000-8000-000000000001', 'R60_SUMMARY_COMPLETION', 'Hoàn thành đoạn tóm tắt', 'STRATEGY', 'PROCEDURE', 'READING', 'Điền từ vào đoạn tóm tắt, đúng ý, đúng dạng từ và đúng giới hạn số từ.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000003', '31000000-0000-4000-8000-000000000002', 'L60_FORM_NOTE_COMPLETION', 'Điền form và ghi chú', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Nghe và điền đúng thông tin vào form hoặc ghi chú theo thứ tự xuất hiện.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000004', '31000000-0000-4000-8000-000000000002', 'L60_SIGNPOSTING', 'Nhận biết từ dẫn ý', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Nhận ra từ và cụm từ báo hiệu chuyển ý để biết đáp án sắp xuất hiện.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000005', '31000000-0000-4000-8000-000000000003', 'W60_PARAGRAPH_COHESION', 'Liên kết trong đoạn văn', 'STRATEGY', 'PROCEDURE', 'WRITING', 'Nối các câu trong đoạn bằng từ nối và tham chiếu để ý đi liền mạch.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000006', '31000000-0000-4000-8000-000000000003', 'W60_T1_KEY_FEATURES', 'Chọn đặc điểm chính Task 1', 'STRATEGY', 'PROCEDURE', 'WRITING', 'Chọn và so sánh các đặc điểm nổi bật của biểu đồ thay vì liệt kê hết số liệu.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000007', '31000000-0000-4000-8000-000000000004', 'R70_WRITER_VIEW_YNNG', 'Quan điểm tác giả (Yes/No/Not Given)', 'STRATEGY', 'PROCEDURE', 'READING', 'Phân biệt quan điểm của tác giả với thông tin được nêu trong bài.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000008', '31000000-0000-4000-8000-000000000004', 'R70_HEDGING_SCOPE', 'Từ giảm nhẹ và phạm vi', 'STRATEGY', 'PROCEDURE', 'READING', 'Nhận ra từ giảm nhẹ và từ giới hạn phạm vi làm đổi nghĩa của nhận định.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000009', '31000000-0000-4000-8000-000000000004', 'R70_COMPLEX_PARAPHRASE', 'Paraphrase phức tạp', 'STRATEGY', 'PROCEDURE', 'READING', 'Nối câu hỏi với đoạn văn khi ý được diễn đạt lại bằng cấu trúc và từ vựng khác hẳn.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000010', '31000000-0000-4000-8000-000000000005', 'L70_MULTI_SPEAKER_ATTITUDE', 'Thái độ nhiều người nói', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Theo dõi thái độ và quan điểm của từng người trong cuộc hội thoại nhiều người.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000011', '31000000-0000-4000-8000-000000000005', 'L70_MAP_DIAGRAM', 'Bản đồ và sơ đồ', 'STRATEGY', 'PROCEDURE', 'LISTENING', 'Theo dõi chỉ dẫn vị trí để gắn đúng nhãn lên bản đồ hoặc sơ đồ.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000012', '31000000-0000-4000-8000-000000000006', 'W70_IDEA_DEVELOPMENT', 'Phát triển ý', 'STRATEGY', 'PROCEDURE', 'WRITING', 'Mở rộng mỗi ý chính bằng giải thích và ví dụ cụ thể, tránh lặp ý.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000013', '31000000-0000-4000-8000-000000000006', 'W70_LEXICAL_PRECISION', 'Dùng từ chính xác', 'STRATEGY', 'PROCEDURE', 'WRITING', 'Chọn từ và cụm từ đúng sắc thái và đúng kết hợp từ cho ngữ cảnh.', 'ACTIVE'),
    ('31000000-0000-4000-8000-020000000014', '31000000-0000-4000-8000-000000000006', 'W70_COMPLEX_STRUCTURES', 'Cấu trúc câu phức', 'STRATEGY', 'PROCEDURE', 'WRITING', 'Dùng đa dạng câu phức và mệnh đề phụ một cách chính xác và tự nhiên.', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Edges are resolved by knowledge point code, so they do not depend on the ids of older seed rows.
WITH edge(prerequisite_code, target_code) AS (VALUES
    ('TR_R_SCANNING', 'PR_SENTENCE_COMPLETION'),
    ('PR_SENTENCE_COMPLETION', 'R60_SUMMARY_COMPLETION'),
    ('DR_TOPIC_SENTENCE', 'DR_MATCHING_HEADINGS'),
    ('DR_MATCHING_HEADINGS', 'R60_SKIM_STRUCTURE'),
    ('TR_R_WORD_IN_CONTEXT', 'R65_PARAPHRASE'),
    ('R65_PARAPHRASE', 'R70_COMPLEX_PARAPHRASE'),
    ('TFNG_FALSE_VS_NOT_GIVEN', 'R65_EVIDENCE_INFERENCE'),
    ('R65_EVIDENCE_INFERENCE', 'R70_WRITER_VIEW_YNNG'),
    ('R65_EVIDENCE_INFERENCE', 'R70_HEDGING_SCOPE'),
    ('LS_NUM', 'L60_FORM_NOTE_COMPLETION'),
    ('LS_SPELL', 'L60_FORM_NOTE_COMPLETION'),
    ('L60_FORM_NOTE_COMPLETION', 'L65_CORRECTION'),
    ('LS_PARA', 'LS_TRAP'),
    ('LS_TRAP', 'L65_CORRECTION'),
    ('TR_L_DETAIL', 'L60_SIGNPOSTING'),
    ('L60_SIGNPOSTING', 'L65_SPEAKER_PURPOSE'),
    ('L65_SPEAKER_PURPOSE', 'L70_MULTI_SPEAKER_ATTITUDE'),
    ('L60_SIGNPOSTING', 'L70_MAP_DIAGRAM'),
    ('TR_W_OPINION', 'W60_PARAGRAPH_COHESION'),
    ('W60_PARAGRAPH_COHESION', 'W65_DISCUSS_BOTH_VIEWS'),
    ('W65_DISCUSS_BOTH_VIEWS', 'W70_IDEA_DEVELOPMENT'),
    ('DEMO_READING_W1_CHART', 'W60_T1_KEY_FEATURES'),
    ('W60_PARAGRAPH_COHESION', 'W70_COMPLEX_STRUCTURES'),
    ('TR_R_WORD_IN_CONTEXT', 'W70_LEXICAL_PRECISION')
)
INSERT INTO knowledge_point_prerequisites (knowledge_point_id, prerequisite_id)
SELECT target.id, prerequisite.id
FROM edge
JOIN knowledge_points prerequisite ON prerequisite.code = edge.prerequisite_code
JOIN knowledge_points target ON target.code = edge.target_code
ON CONFLICT DO NOTHING;
