-- Follow-up to the examiner draft of 2026-11-12, decided by the product owner without a human examiner review (the draft
-- was written by an AI standing in for one), so names and hints can still be changed by a later migration.
--
-- 1. WORD_FORM: a word of the wrong class (more convenience, a naturally process). It turned up in 2 of the 8 gold essays
--    and has no close code: WORD_CHOICE is about meaning, not word class.
-- 2. T1_DATA_MISREPORTED also covers wrong process steps and wrong changes between two maps, not only figures.
-- 3. VERB_FORM names the wrong past form of an irregular verb (growed, rised), which sat between TENSE and VERB_FORM.
-- 4. T2_IRRELEVANT_EXAMPLE keeps only examples that do not fit; an example that is too general belongs to
--    UNDERDEVELOPED_IDEA.
--
-- WORD_FORM gets no error-trap point for the same reason as the codes of V125: a trap point needs two published practice
-- sets with trap questions, and those learner-facing questions are not written.
INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, skills, fallback)
SELECT 'WORD_FORM', 'Sai từ loại', 'GRAMMAR', 1, kp.id,
       'Xác định vị trí của từ trong câu cần loại từ nào rồi chọn đúng dạng: tính từ bổ nghĩa danh từ (convenient), trạng từ bổ nghĩa động từ hoặc tính từ (naturally), danh từ làm chủ ngữ hoặc tân ngữ (convenience).',
       'ACTIVE', 'APPROVED', ARRAY['WRITING'], false
FROM knowledge_points kp
WHERE kp.code = 'V55_WORD_FORMS'
ON CONFLICT (code) DO NOTHING;

UPDATE error_types
SET name = 'Báo cáo sai nội dung Task 1',
    fix_hint = 'Đối chiếu với đề từng số liệu, xu hướng, thứ tự các bước của quy trình hoặc thay đổi giữa hai bản đồ, đặc biệt khi so sánh hai mốc thời gian.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'T1_DATA_MISREPORTED';

UPDATE error_types
SET fix_hint = 'Sau can, could, should dùng động từ nguyên mẫu; không đặt am, is, are trước động từ thường; dùng V-ing khi hành động làm chủ ngữ; chia đúng dạng quá khứ của động từ bất quy tắc (grew, rose, không phải growed, rised).',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'VERB_FORM';

UPDATE error_types
SET name = 'Ví dụ không liên quan',
    fix_hint = 'Chọn ví dụ minh họa đúng ý của câu chủ đề; bỏ ví dụ không gắn trực tiếp với lập luận.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'T2_IRRELEVANT_EXAMPLE';

UPDATE error_types
SET fix_hint = 'Mỗi ý chính cần một lý do và một ví dụ cụ thể; ví dụ quá chung chung, không nêu được người, nơi hay số liệu, chưa đủ.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'UNDERDEVELOPED_IDEA';

-- Refuse to finish unless WORD_FORM exists for Writing with an active point under an active topic and course, so the
-- practice button never leads a learner to content they cannot open.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM error_types WHERE code = 'WORD_FORM') THEN
        RAISE EXCEPTION 'WORD_FORM was not created: the knowledge point V55_WORD_FORMS is missing';
    END IF;
    IF EXISTS (
        SELECT 1 FROM error_types et
        LEFT JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
        LEFT JOIN topics t ON t.id = parent.topic_id
        LEFT JOIN courses c ON c.id = t.course_id
        WHERE et.code = 'WORD_FORM'
          AND (parent.id IS NULL OR parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE'
               OR NOT ('WRITING' = ANY (et.skills)))
    ) THEN
        RAISE EXCEPTION 'WORD_FORM must map to an active point under an active topic and course';
    END IF;
END $$;
