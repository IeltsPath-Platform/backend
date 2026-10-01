-- A hint points the learner back to where to reread after a wrong answer; it never states the answer. Learning
-- Service shows it only for questions with at least three choices or a fill answer, after a wrong attempt.
ALTER TABLE question_versions ADD COLUMN hint TEXT;

-- Reading lesson questions of the demo seed. Q3 (two choices) has no hint.
UPDATE question_versions SET hint = 'Câu chủ đề nêu ý chung của cả đoạn B. Câu có số liệu hoặc bắt đầu bằng "As a result" thường là bằng chứng hay hệ quả.'
WHERE id = '20000000-0000-4000-8000-070000000001'; -- Q13
UPDATE question_versions SET hint = 'Tìm câu mà các câu còn lại của đoạn C đều giải thích hoặc minh họa cho nó. Ví dụ về một thành phố cụ thể hiếm khi là câu chủ đề.'
WHERE id = '20000000-0000-4000-8000-070000000002'; -- Q1
UPDATE question_versions SET hint = 'Câu báo trước nội dung cả đoạn D thường ngắn và khái quát. Câu nêu một lý do cụ thể chỉ là chi tiết.'
WHERE id = '20000000-0000-4000-8000-070000000003'; -- Q11
UPDATE question_versions SET hint = 'Đọc câu thứ hai của đoạn D. Từ cần tìm là danh từ số nhiều chỉ người, đứng đầu câu.'
WHERE id = '20000000-0000-4000-8000-070000000004'; -- Q12
UPDATE question_versions SET hint = 'Ý chính của cả bài phải đúng với mọi đoạn, kể cả đoạn D. Loại phương án chỉ khớp với một chi tiết.'
WHERE id = '20000000-0000-4000-8000-070000000005'; -- Q5
UPDATE question_versions SET hint = 'Đọc câu đầu đoạn C, rồi chọn heading tóm được cả đoạn, không chỉ một ví dụ.'
WHERE id = '20000000-0000-4000-8000-070000000007'; -- Q4
UPDATE question_versions SET hint = 'So từng chi tiết của câu khẳng định với đoạn văn, nhất là ngày trong tuần.'
WHERE id = '20000000-0000-4000-8000-070000000008'; -- QT1
