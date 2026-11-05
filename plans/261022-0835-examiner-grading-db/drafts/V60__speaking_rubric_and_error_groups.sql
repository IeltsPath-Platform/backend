-- DRAFT, NOT IN THE MIGRATION FOLDER: ship together with the Content Service code change, never alone.
-- Content Service code still rejects what this migration adds: Rubric and ManageRubricsUseCase accept only TASK_1 and
-- TASK_2, JdbcRubricRepository expects the Writing criteria when it activates a rubric, and the ErrorCategory enum has no
-- PRONUNCIATION or FLUENCY. Rows added here would make GET /api/content/rubrics and GET /api/content/error-types fail.
--
-- Adds what grading a Speaking response needs: a Speaking rubric (kept DRAFT, so no active rubric changes), the criteria
-- codes FC and PR, and two error groups with their first codes (DRAFT review status, so learners and graders do not see
-- them until they are approved).

DO $$
DECLARE
    old_constraint TEXT;
BEGIN
    FOR old_constraint IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'rubrics'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) ILIKE '%TASK_1%' LOOP
        EXECUTE format('ALTER TABLE rubrics DROP CONSTRAINT %I', old_constraint);
    END LOOP;
    FOR old_constraint IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'rubric_criteria'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) ILIKE '%GRA%' LOOP
        EXECUTE format('ALTER TABLE rubric_criteria DROP CONSTRAINT %I', old_constraint);
    END LOOP;
    FOR old_constraint IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'error_types'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) ILIKE '%READING_STRATEGY%' LOOP
        EXECUTE format('ALTER TABLE error_types DROP CONSTRAINT %I', old_constraint);
    END LOOP;
END $$;

ALTER TABLE rubrics ADD CONSTRAINT chk_rubrics_task CHECK (task IN ('TASK_1', 'TASK_2', 'SPEAKING'));
ALTER TABLE rubric_criteria ADD CONSTRAINT chk_rubric_criteria_code
    CHECK (code IN ('TR', 'TA', 'CC', 'LR', 'GRA', 'FC', 'PR'));
ALTER TABLE error_types ADD CONSTRAINT chk_error_types_category CHECK (category IN
    ('GRAMMAR', 'VOCABULARY', 'COHERENCE', 'TASK', 'READING_STRATEGY', 'LISTENING_STRATEGY', 'SPELLING_NUMBER',
     'PRONUNCIATION', 'FLUENCY'));

-- The descriptors are original paraphrases of the public IELTS Speaking band descriptors. They are learning and
-- calibration notes, not reproduced official wording.
INSERT INTO rubrics (id, code, task, rubric_version, status, source_note) VALUES
    ('39a00000-0000-4000-8000-000000000003', 'IELTS_SPEAKING', 'SPEAKING', 1, 'DRAFT',
     'Original paraphrase for calibration, based on the public IELTS Speaking band descriptors (ielts.org). Requires review by an IELTS-qualified examiner; this AI-authored draft has not yet been reviewed. Stays DRAFT until Content Service can activate a Speaking rubric.');

INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order) VALUES
    ('39a00000-0000-4000-8000-000000000003', 'FC', 'Fluency and Coherence',
     'How smoothly and at what length the candidate speaks, and how clearly ideas are ordered and linked.', 1),
    ('39a00000-0000-4000-8000-000000000003', 'LR', 'Lexical Resource',
     'Range, precision and flexibility of vocabulary, including paraphrase and less common items.', 2),
    ('39a00000-0000-4000-8000-000000000003', 'GRA', 'Grammatical Range and Accuracy',
     'Range of sentence forms and control of grammar while speaking.', 3),
    ('39a00000-0000-4000-8000-000000000003', 'PR', 'Pronunciation',
     'How easily the candidate can be understood: individual sounds, word and sentence stress, intonation and rhythm.', 4);

WITH bands(band) AS (VALUES (4.0),(4.5),(5.0),(5.5),(6.0),(6.5),(7.0),(7.5),(8.0),(8.5),(9.0))
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT c.id, b.band,
       CASE b.band
         WHEN 4.0 THEN 'Performance is limited: answers are short or broken, and recurring problems can make meaning difficult to follow.'
         WHEN 4.5 THEN 'Performance remains limited, though some answers are recognizable; frequent problems interrupt meaning.'
         WHEN 5.0 THEN 'Performance is basic and uneven: the candidate keeps going but with repetition, hesitation or limited control.'
         WHEN 5.5 THEN 'Performance is developing: answers are mostly sustained, but problems remain noticeable and uneven.'
         WHEN 6.0 THEN 'Performance is generally effective: the candidate is willing to speak at length, though some lapses remain.'
         WHEN 6.5 THEN 'Performance is mostly effective and clear, with occasional lapses that stop it from being consistently strong.'
         WHEN 7.0 THEN 'Performance is clear and well controlled: the candidate speaks at length with only occasional lapses or imprecision.'
         WHEN 7.5 THEN 'Performance is strong: speech is well controlled and varied, with only minor lapses.'
         WHEN 8.0 THEN 'Performance is strong and precise, with only rare slips and little effort to follow.'
         WHEN 8.5 THEN 'Performance is very strong and flexible, with only occasional minor slips.'
         ELSE 'Performance is consistently precise, natural and flexible throughout.'
       END || ' ' || CASE c.code
         WHEN 'FC' THEN 'Judge pace, pauses, repetition or self-correction, and how well ideas are extended and linked.'
         WHEN 'LR' THEN 'Judge whether word choice is varied and precise enough for the topic, and how the candidate copes when a word is missing.'
         WHEN 'GRA' THEN 'Judge the mix of simple and complex sentences and how often errors reduce understanding.'
         ELSE 'Judge how easily speech is understood, including stress, intonation and the clarity of individual sounds.'
       END
FROM rubric_criteria c CROSS JOIN bands b
WHERE c.rubric_id = '39a00000-0000-4000-8000-000000000003';

INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status) VALUES
    ('PRON_WORD_STRESS', 'Nhấn sai âm tiết', 'PRONUNCIATION', 2, NULL,
        'Tra từ điển để biết âm tiết được nhấn, rồi nghe và nhắc lại cả từ.', 'ACTIVE'),
    ('PRON_SOUND', 'Phát âm sai âm', 'PRONUNCIATION', 2, NULL,
        'Nghe mẫu và luyện riêng âm bị nhầm, đặc biệt các cặp âm gần nhau.', 'ACTIVE'),
    ('PRON_INTONATION', 'Ngữ điệu đều, thiếu nhấn ý chính', 'PRONUNCIATION', 1, NULL,
        'Nhấn vào từ mang ý chính và lên xuống giọng theo ý định diễn đạt.', 'ACTIVE'),
    ('FLUENCY_FILLER', 'Dùng nhiều từ đệm', 'FLUENCY', 1, NULL,
        'Thay "um", "uh" bằng một nhịp dừng ngắn hoặc cụm nối như "let me think".', 'ACTIVE'),
    ('FLUENCY_LONG_PAUSE', 'Ngập ngừng dài khi tìm ý', 'FLUENCY', 2, NULL,
        'Chuẩn bị sẵn ý chính cho các chủ đề quen thuộc và luyện nói liền mạch theo dàn ý.', 'ACTIVE'),
    ('FLUENCY_SELF_CORRECTION', 'Tự sửa lỗi làm đứt mạch', 'FLUENCY', 1, NULL,
        'Lỗi nhỏ thì nói tiếp, chỉ sửa khi lỗi làm sai nghĩa.', 'ACTIVE');
