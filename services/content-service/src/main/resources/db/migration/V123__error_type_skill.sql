-- Which skills an error type applies to; an array because one grammar code can serve both Writing and Speaking.
ALTER TABLE error_types
    ADD COLUMN skills VARCHAR(20)[] NOT NULL DEFAULT '{}',
    ADD COLUMN fallback BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE error_types ADD CONSTRAINT chk_error_types_skills
    CHECK (skills <@ ARRAY['READING', 'LISTENING', 'WRITING', 'SPEAKING']::varchar[]);

-- Backfill from the existing codes. Speaking codes are SPK_* whatever their category.
UPDATE error_types SET skills = ARRAY['SPEAKING'] WHERE code LIKE 'SPK\_%';
UPDATE error_types SET skills = ARRAY['READING'] WHERE category = 'READING_STRATEGY' AND code NOT LIKE 'SPK\_%';
UPDATE error_types SET skills = ARRAY['LISTENING'] WHERE category = 'LISTENING_STRATEGY' AND code NOT LIKE 'SPK\_%';
UPDATE error_types SET skills = ARRAY['WRITING'] WHERE code IN (
    'TENSE', 'ARTICLE', 'SUBJECT_VERB_AGREEMENT', 'SENTENCE_FRAGMENT',
    'WORD_CHOICE', 'COLLOCATION',
    'LINKER_MISUSE', 'NO_CLEAR_POSITION',
    'UNDERDEVELOPED_IDEA', 'OFF_TOPIC', 'T1_NO_OVERVIEW', 'T1_DATA_MISREPORTED',
    'SPELLING', 'PLURAL_FORM');

-- Fallbacks catch a Writing error that no specific code fits. They have no knowledge point, so they never create
-- evidence or a review item. Approved here because they only ever name a broad class of error.
INSERT INTO error_types (id, code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, skills, fallback)
VALUES
    (gen_random_uuid(), 'GRAMMAR_OTHER', 'Lỗi ngữ pháp khác', 'GRAMMAR', 1, NULL,
     'Đối chiếu lại câu với quy tắc ngữ pháp đã học.', 'ACTIVE', 'APPROVED', ARRAY['WRITING'], true),
    (gen_random_uuid(), 'VOCABULARY_OTHER', 'Lỗi từ vựng khác', 'VOCABULARY', 1, NULL,
     'Tra lại nghĩa và cách dùng của từ trong ngữ cảnh.', 'ACTIVE', 'APPROVED', ARRAY['WRITING'], true),
    (gen_random_uuid(), 'COHERENCE_OTHER', 'Lỗi mạch lạc khác', 'COHERENCE', 1, NULL,
     'Kiểm tra các ý đã nối với nhau bằng từ nối và cấu trúc phù hợp chưa.', 'ACTIVE', 'APPROVED', ARRAY['WRITING'], true),
    (gen_random_uuid(), 'TASK_OTHER', 'Lỗi đáp ứng yêu cầu đề khác', 'TASK', 1, NULL,
     'Đọc lại đề và kiểm tra bài đã trả lời đúng yêu cầu chưa.', 'ACTIVE', 'APPROVED', ARRAY['WRITING'], true)
ON CONFLICT (code) DO NOTHING;
