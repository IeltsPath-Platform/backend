-- The Speaking grader may only report an error whose code starts with SPK_ (anything else is dropped), and until now GRA had a
-- single such code (only simple sentences) and LR three (vague word, collocation, no paraphrase). Tense, article, agreement,
-- plural and past endings, prepositions, word order and a wrong word could therefore not be listed as errors with their quote
-- and their correction, even when the grader had found them.
-- This migration adds one code for each of those mistakes so the grader can list every place a mistake happens, each with the
-- exact words and the fix. Like the codes of V115 they are linked to the Speaking grammar and vocabulary points and carry no
-- trap point, so they only explain a result: they do not add evidence against any knowledge point.
-- The codes are written for practice feedback and still deserve a review by an IELTS-qualified examiner.

INSERT INTO error_types (id, code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, trap_knowledge_point_id, created_at, updated_at)
SELECT gen_random_uuid(), t.code, t.name, t.category, t.severity, kp.id, t.fix_hint, 'ACTIVE', 'APPROVED', NULL, now(), now()
  FROM (VALUES
    ('SPK_TENSE_ERROR',            'Sai thì của động từ',                              'GRAMMAR',    2, 'S65_GRAMMAR_RANGE',       'Xác định mốc thời gian (yesterday, last year, now, since) rồi chọn thì: chuyện đã xong dùng quá khứ đơn ("I went"), chuyện kéo dài đến nay dùng hiện tại hoàn thành ("I have lived here for three years").'),
    ('SPK_VERB_FORM',              'Sai dạng động từ (sau to, V-ing, quá khứ bất quy tắc)', 'GRAMMAR', 2, 'S65_GRAMMAR_RANGE',   'Sau "to" dùng động từ nguyên mẫu ("want to go"), sau enjoy hoặc avoid dùng V-ing, và học dạng quá khứ bất quy tắc ("went", "saw", "bought").'),
    ('SPK_SUBJECT_VERB_AGREEMENT', 'Sai hòa hợp chủ ngữ và động từ',                   'GRAMMAR',    2, 'S65_GRAMMAR_RANGE',       'Chủ ngữ số ít ngôi thứ ba thêm -s ("she likes", "it helps"), chủ ngữ số nhiều thì không ("they like").'),
    ('SPK_PLURAL_ENDING',          'Thiếu hoặc sai đuôi số nhiều',                     'GRAMMAR',    1, 'S65_GRAMMAR_RANGE',       'Danh từ đếm được số nhiều cần -s hoặc -es ("three friends", "two buses"); nhớ các dạng bất quy tắc như "children", "people".'),
    ('SPK_ARTICLE_ERROR',          'Thiếu hoặc dùng sai mạo từ (a, an, the)',          'GRAMMAR',    1, 'S65_GRAMMAR_RANGE',       'Dùng "the" khi người nghe đã biết đang nói đến cái nào; dùng "a/an" khi nhắc lần đầu một danh từ đếm được số ít; không dùng mạo từ khi nói chung chung với danh từ số nhiều.'),
    ('SPK_PREPOSITION_ERROR',      'Dùng sai giới từ',                                 'GRAMMAR',    1, 'S65_GRAMMAR_RANGE',       'Học giới từ theo cụm: "interested in", "good at", "depend on", "in the morning", "on Monday", "at night".'),
    ('SPK_WORD_ORDER',             'Sai trật tự từ',                                   'GRAMMAR',    2, 'S65_GRAMMAR_RANGE',       'Giữ trật tự chủ ngữ, động từ, tân ngữ; tính từ đứng trước danh từ ("a big house"); câu hỏi đưa trợ động từ lên trước chủ ngữ ("Where do you live?").'),
    ('SPK_MISSING_SUBJECT_VERB',   'Câu thiếu chủ ngữ, động từ hoặc "to be"',          'GRAMMAR',    2, 'S65_GRAMMAR_RANGE',       'Mỗi câu cần chủ ngữ và động từ chính; nhớ am, is, are trước tính từ ("She is tired", không nói "She tired").'),
    ('SPK_RUN_ON_SENTENCE',        'Câu dài nối sai hoặc bỏ dở',                       'GRAMMAR',    1, 'S65_GRAMMAR_RANGE',       'Tách thành hai câu hoặc nối bằng từ nối đúng chức năng (and, but, because, so, which) thay vì nói liền một mạch; hoàn thành ý trước khi sang ý mới.'),
    ('SPK_COMPLEX_STRUCTURE_ERROR','Sai trong câu phức (quan hệ, điều kiện, bị động, so sánh)', 'GRAMMAR', 2, 'S65_GRAMMAR_RANGE', 'Kiểm tra từng phần của cấu trúc: "If + hiện tại, will + V", "the man who lives next door", "was built in 1990", "more expensive than".'),
    ('SPK_WORD_CHOICE',            'Chọn từ sai nghĩa hoặc sai sắc thái',              'VOCABULARY', 2, 'S60_SPEAKING_VOCABULARY', 'Kiểm tra nghĩa và sắc thái của từ trong ngữ cảnh, tra từ điển có câu ví dụ, và chọn từ đúng nghĩa thay vì từ gần nghĩa.'),
    ('SPK_WORD_FORM',              'Sai dạng từ (danh từ, động từ, tính từ, trạng từ)','VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY', 'Học cả họ từ: "important, importance", "interest, interested, interesting"; chọn dạng đúng theo vị trí của từ trong câu.'),
    ('SPK_L1_TRANSLATION',         'Dịch từng chữ từ tiếng Việt',                      'VOCABULARY', 2, 'S60_SPEAKING_VOCABULARY', 'Học cả cụm tiếng Anh thay vì dịch từng chữ, ví dụ nói "turn on the light" chứ không phải "open the light".'),
    ('SPK_REPETITIVE_WORD',        'Lặp lại cùng một từ nhiều lần',                    'VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY', 'Chuẩn bị hai hoặc ba từ thay thế cho từ hay lặp: "like" thành "enjoy" hoặc "be keen on"; "important" thành "essential" hoặc "crucial".'),
    ('SPK_MISUSED_IDIOM',          'Dùng thành ngữ hoặc cụm học thuộc sai hoặc gượng', 'VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY', 'Chỉ dùng thành ngữ khi chắc chắn về nghĩa và ngữ cảnh; một cụm tự nhiên và đúng tốt hơn một thành ngữ gượng.'),
    ('SPK_WRONG_REGISTER',         'Dùng văn phong không phù hợp khi nói',             'VOCABULARY', 1, 'S60_SPEAKING_VOCABULARY', 'Khi nói, dùng ngôn ngữ tự nhiên và trung tính; tránh từ quá sách vở dùng liên tục ("moreover", "furthermore") hoặc tiếng lóng.')
  ) AS t(code, name, category, severity, kp_code, fix_hint)
  JOIN knowledge_points kp ON kp.code = t.kp_code
 WHERE NOT EXISTS (SELECT 1 FROM error_types e WHERE e.code = t.code);

DO $$
BEGIN
    IF (SELECT count(*) FROM error_types
         WHERE status = 'ACTIVE' AND code IN (
            'SPK_TENSE_ERROR', 'SPK_VERB_FORM', 'SPK_SUBJECT_VERB_AGREEMENT', 'SPK_PLURAL_ENDING', 'SPK_ARTICLE_ERROR',
            'SPK_PREPOSITION_ERROR', 'SPK_WORD_ORDER', 'SPK_MISSING_SUBJECT_VERB', 'SPK_RUN_ON_SENTENCE',
            'SPK_COMPLEX_STRUCTURE_ERROR', 'SPK_WORD_CHOICE', 'SPK_WORD_FORM', 'SPK_L1_TRANSLATION', 'SPK_REPETITIVE_WORD',
            'SPK_MISUSED_IDIOM', 'SPK_WRONG_REGISTER')) <> 16 THEN
        RAISE EXCEPTION 'All 16 Speaking error codes must exist and be active (are the Speaking knowledge points present?)';
    END IF;
END $$;
