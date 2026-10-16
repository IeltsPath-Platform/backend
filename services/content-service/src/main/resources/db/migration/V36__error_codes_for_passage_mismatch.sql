-- Three error codes approved in the phase-03 review, for wrong options that no earlier code describes, and the six
-- options they apply to. Each update is guarded by the correct key, and each merges into the existing distractors map.

INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status) VALUES
    ('CONTRADICTED_BY_PASSAGE', 'Phương án ngược với bài', 'READING_STRATEGY', 2, NULL,
        'Phương án này ngược với bài. Đọc lại câu gốc để xem bài khẳng định điều gì.', 'ACTIVE'),
    ('DETAIL_AS_MAIN_IDEA', 'Lấy chi tiết làm ý chính', 'READING_STRATEGY', 2,
        (SELECT id FROM knowledge_points WHERE code = 'DR_IDEA_OR_DETAIL'),
        'Đây là một chi tiết hỗ trợ, không phải ý chính. Tìm câu nêu ý chung của cả đoạn.', 'ACTIVE'),
    ('WRONG_TIME_FRAME', 'Gắn ý với mốc thời gian sai', 'READING_STRATEGY', 2, NULL,
        'Bài không nói về thời điểm này. Kiểm tra mốc thời gian trong đoạn trước khi chọn.', 'ACTIVE');

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"A": "CONTRADICTED_BY_PASSAGE"}'::jsonb)
WHERE id = '26000000-0000-4000-8000-070000000001' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"A": "CONTRADICTED_BY_PASSAGE"}'::jsonb)
WHERE id = '33000000-0000-4000-8000-070000000001' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"A": "CONTRADICTED_BY_PASSAGE"}'::jsonb)
WHERE id = '27000000-0000-4000-8000-070000000015' AND answer_spec->>'correct' = 'C';

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"B": "CONTRADICTED_BY_PASSAGE"}'::jsonb)
WHERE id = '33000000-0000-4000-8000-070000000002' AND answer_spec->>'correct' = 'A';

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"B": "DETAIL_AS_MAIN_IDEA"}'::jsonb)
WHERE id = '20000000-0000-4000-8000-070000000003' AND answer_spec->>'correct' = 'A';

UPDATE question_versions
SET answer_spec = jsonb_set(answer_spec, '{distractors}',
    COALESCE(answer_spec->'distractors', '{}'::jsonb) || '{"B": "WRONG_TIME_FRAME"}'::jsonb)
WHERE id = '27000000-0000-4000-8000-070000000010' AND answer_spec->>'correct' = 'A';
