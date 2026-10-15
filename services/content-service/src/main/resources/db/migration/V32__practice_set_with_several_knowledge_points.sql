-- PS-KP3-B (Urban cycling) mixes three knowledge points of the Reading topic instead of testing one: paragraph A
-- stays a topic-sentence question (DR_TOPIC_SENTENCE), paragraph B becomes an idea-or-detail question
-- (DR_IDEA_OR_DETAIL) and paragraph C a heading question (DR_MATCHING_HEADINGS). A failed attempt now scores each
-- knowledge point on its own, so only the weak ones get a review.

UPDATE question_versions SET
    stem = '"Regular riders have lower rates of heart disease." is…',
    options = '[{"optionKey": "MAIN_IDEA", "content": "a main idea", "sortOrder": 1}, {"optionKey": "DETAIL", "content": "a supporting detail", "sortOrder": 2}]'::jsonb,
    answer_spec = '{"type": "CHOICE", "correct": "DETAIL"}'::jsonb,
    explanation = 'Đây là số liệu minh họa cho ý chung "Cycling brings clear health benefits", không phải ý chính của đoạn B.',
    hint = 'Câu này nêu một kết quả cụ thể hay một nhận định bao trùm cả đoạn?'
WHERE id = '27000000-0000-4000-8000-070000000002';

UPDATE question_versions SET
    stem = 'Choose the best heading for paragraph C.',
    options = '[{"optionKey": "i", "content": "A change not everyone welcomes", "sortOrder": 1}, {"optionKey": "ii", "content": "Cities that ban cars", "sortOrder": 2}, {"optionKey": "iii", "content": "The health benefits of a daily ride", "sortOrder": 3}, {"optionKey": "iv", "content": "How to build a cycle lane", "sortOrder": 4}]'::jsonb,
    answer_spec = '{"type": "CHOICE", "correct": "i"}'::jsonb,
    explanation = 'Đoạn C nêu chủ cửa hàng và tài xế phản đối việc thêm làn xe đạp, nên "A change not everyone welcomes" khớp cả đoạn.',
    hint = 'Heading phải bao được cả hai nhóm người trong đoạn, không chỉ một ví dụ.'
WHERE id = '27000000-0000-4000-8000-070000000003';

DELETE FROM question_knowledge_points
WHERE question_version_id IN ('27000000-0000-4000-8000-070000000002', '27000000-0000-4000-8000-070000000003');

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight) VALUES
    ('27000000-0000-4000-8000-070000000002', '20000000-0000-4000-8000-020000000002', 1.00),
    ('27000000-0000-4000-8000-070000000003', '20000000-0000-4000-8000-020000000004', 1.00);
