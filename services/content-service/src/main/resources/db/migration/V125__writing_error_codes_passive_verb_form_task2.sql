-- Writing error codes for errors the catalog had no code for, found while marking the examiner essay set: passive
-- voice, verb form, prepositions and comparisons; plus three Task 2 task-response codes, and a knowledge point for
-- NO_CLEAR_POSITION. Decided by the product owner on 2026-11-11 without a separate examiner review; names, hints and
-- points can be changed later by a new migration.
--
-- These codes get no error-trap point yet. Every code with a trap point must have two published practice sets with
-- trap questions (ErrorTrapCoverageMigrationTest), and those learner-facing questions are not written. The trap points
-- are added by the migration that adds the sets.
INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, skills, fallback)
SELECT v.code, v.name, v.category, v.severity, kp.id, v.fix_hint, 'ACTIVE', 'APPROVED', ARRAY['WRITING'], false
FROM (VALUES
    ('PASSIVE_VOICE', 'Sai cấu trúc bị động', 'GRAMMAR', 2, 'G55_PASSIVE_VOICE',
     'Bị động gồm be chia theo thì và chủ ngữ, cộng quá khứ phân từ: is collected, was located, are removed.'),
    ('VERB_FORM', 'Sai dạng động từ', 'GRAMMAR', 2, 'G55_BASIC_SENTENCE_PATTERNS',
     'Sau can, could, should dùng động từ nguyên mẫu; không đặt am, is, are trước động từ thường; dùng V-ing khi hành động làm chủ ngữ.'),
    ('PREPOSITION', 'Sai giới từ', 'GRAMMAR', 1, 'V55_ACADEMIC_COLLOCATIONS',
     'Học giới từ đi kèm theo cả cụm: increase by, despite + danh từ (không có of), the number of.'),
    ('COMPARISON', 'Sai cấu trúc so sánh', 'GRAMMAR', 1, 'G55_COMPARISON',
     'So sánh hơn dùng than, không phải then; không ghép more với dạng -er như more wider.'),
    ('T2_PARTIAL_ANSWER', 'Trả lời thiếu phần của đề Task 2', 'TASK', 3, 'W65_DISCUSS_BOTH_VIEWS',
     'Gạch dưới từng yêu cầu của đề (mỗi câu hỏi, mỗi quan điểm) và kiểm tra bài có đoạn trả lời cho từng yêu cầu.'),
    ('T2_NO_CONCLUSION', 'Thiếu kết bài Task 2', 'TASK', 2, 'TR_W_OPINION',
     'Kết thúc bằng một đoạn ngắn nhắc lại quan điểm và các lý do chính, không thêm ý mới.'),
    ('T2_IRRELEVANT_EXAMPLE', 'Ví dụ không liên quan hoặc quá chung chung', 'TASK', 2, 'W70_IDEA_DEVELOPMENT',
     'Chọn ví dụ cụ thể minh họa đúng ý của câu chủ đề; bỏ ví dụ không gắn trực tiếp với lập luận.')
) AS v(code, name, category, severity, kp_code, fix_hint)
JOIN knowledge_points kp ON kp.code = v.kp_code
ON CONFLICT (code) DO NOTHING;

-- NO_CLEAR_POSITION had no point, so the practice button could not open anything for it.
UPDATE error_types
SET knowledge_point_id = (SELECT id FROM knowledge_points WHERE code = 'TR_W_OPINION'),
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'NO_CLEAR_POSITION' AND knowledge_point_id IS NULL;

-- Refuse to finish unless every code exists for Writing with an active point under an active topic and course, so no
-- code points a learner at content they cannot open.
DO $$
BEGIN
    IF (SELECT count(*) FROM error_types WHERE code IN ('PASSIVE_VOICE', 'VERB_FORM', 'PREPOSITION', 'COMPARISON',
            'T2_PARTIAL_ANSWER', 'T2_NO_CONCLUSION', 'T2_IRRELEVANT_EXAMPLE', 'NO_CLEAR_POSITION')) <> 8 THEN
        RAISE EXCEPTION 'expected 8 Writing error codes after V125';
    END IF;
    IF EXISTS (
        SELECT 1 FROM error_types et
        LEFT JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
        LEFT JOIN topics t ON t.id = parent.topic_id
        LEFT JOIN courses c ON c.id = t.course_id
        WHERE et.code IN ('PASSIVE_VOICE', 'VERB_FORM', 'PREPOSITION', 'COMPARISON',
                          'T2_PARTIAL_ANSWER', 'T2_NO_CONCLUSION', 'T2_IRRELEVANT_EXAMPLE', 'NO_CLEAR_POSITION')
          AND (parent.id IS NULL OR parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE'
               OR NOT ('WRITING' = ANY (et.skills)))
    ) THEN
        RAISE EXCEPTION 'new Writing error codes must map to an active point under an active topic and course';
    END IF;
END $$;
