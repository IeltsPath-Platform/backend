-- Writing error codes for the grader. Each code gets a definition the model reads (model_definition, English, with a
-- "use when" and a "do not use when" line) and a task filter (writing_tasks: an empty array means both tasks). fix_hint
-- stays the Vietnamese advice shown to the learner; the model never sees it.
--
-- SPELLING and PLURAL_FORM are also tagged automatically on Listening and Reading gap fills, so they keep their code,
-- category, point and hint, and only lose the WRITING skill. Writing uses WRITING_SPELLING and WRITING_PLURAL_FORM.
--
-- Definitions and names are drafts until an IELTS examiner reviews them; a later migration can change them.

ALTER TABLE error_types
    ADD COLUMN model_definition TEXT,
    ADD COLUMN writing_tasks VARCHAR(10)[] NOT NULL DEFAULT '{}';

ALTER TABLE error_types ADD CONSTRAINT chk_error_types_writing_tasks
    CHECK (writing_tasks <@ ARRAY['TASK_1', 'TASK_2']::varchar[]);

UPDATE error_types
SET skills = array_remove(skills, 'WRITING'::varchar), updated_at = CURRENT_TIMESTAMP
WHERE code IN ('SPELLING', 'PLURAL_FORM');

UPDATE error_types SET category = 'TASK', writing_tasks = ARRAY['TASK_2']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code = 'NO_CLEAR_POSITION';

UPDATE error_types SET category = 'VOCABULARY', updated_at = CURRENT_TIMESTAMP
WHERE code = 'PREPOSITION';

UPDATE error_types
SET knowledge_point_id = (SELECT id FROM knowledge_points WHERE code = 'TR_W_OPINION'),
    writing_tasks = ARRAY['TASK_2']::varchar[],
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'OFF_TOPIC';

UPDATE error_types
SET fix_hint = 'Kiểm tra dạng động từ: sau trợ động từ và modal dùng nguyên mẫu, không đặt is/are trước động từ thường, dùng đúng dạng quá khứ (grew, rose, không phải growed, rised).',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'VERB_FORM';

UPDATE error_types SET writing_tasks = ARRAY['TASK_1']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code IN ('T1_NO_OVERVIEW', 'T1_DATA_MISREPORTED');

UPDATE error_types SET writing_tasks = ARRAY['TASK_2']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code IN ('T2_PARTIAL_ANSWER', 'T2_IRRELEVANT_EXAMPLE', 'UNDERDEVELOPED_IDEA');

-- The new codes. Each is approved, active and Writing-only, and gets no error-trap point yet (the same rule as the
-- codes of the earlier Writing migrations: a trap point needs published practice sets with trap questions).
-- WRITING_SPELLING has no knowledge point: no practice set exists for Writing spelling yet, so it creates no evidence.
INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, skills,
                         fallback, writing_tasks)
SELECT v.code, v.name, v.category, v.severity, kp.id, v.fix_hint, 'ACTIVE', 'APPROVED', ARRAY['WRITING']::varchar[],
       false, v.tasks
FROM (VALUES
    ('T1_IRRELEVANT_CONTENT', 'Ý kiến hoặc dự đoán không có trong biểu đồ', 'TASK', 2, 'W60_T1_KEY_FEATURES',
     'Bài Task 1 chỉ mô tả những gì biểu đồ cho thấy. Bỏ ý kiến, lý do hay dự đoán không có trong biểu đồ.',
     ARRAY['TASK_1']::varchar[]),
    ('DATA_LANGUAGE', 'Dùng sai từ chỉ xu hướng hoặc mức độ', 'VOCABULARY', 1, 'V60_DATA_TREND_WORDS',
     'Dùng đúng từ chỉ xu hướng và mức độ: rose sharply, fell slightly, stayed steady; không dùng very big để nói về mức tăng.',
     ARRAY['TASK_1']::varchar[]),
    ('MISSING_VERB', 'Thiếu động từ chia trong mệnh đề', 'GRAMMAR', 2, 'G55_BASIC_SENTENCE_PATTERNS',
     'Mỗi mệnh đề cần một động từ chia: they made only 6 visits, không phải they only 6 visit.',
     '{}'::varchar[]),
    ('PERFECT_TENSE', 'Sai hoặc thiếu thì hoàn thành', 'GRAMMAR', 2, 'G60_PERFECT_TENSES',
     'Dùng hiện tại hoàn thành cho việc bắt đầu trong quá khứ và còn kéo dài tới nay: since 2000, it has increased; không dùng với mốc thời gian đã kết thúc.',
     '{}'::varchar[]),
    ('RUN_ON_SENTENCE', 'Hai mệnh đề độc lập nối bằng dấu phẩy', 'GRAMMAR', 2, 'W50_COMPOUND_SENTENCES',
     'Nối hai mệnh đề độc lập bằng liên từ (and, but) hoặc dấu chấm phẩy, không chỉ bằng dấu phẩy.',
     '{}'::varchar[]),
    ('REFERENCE', 'Đại từ hoặc từ thay thế không rõ nghĩa', 'COHERENCE', 1, 'W60_PARAGRAPH_COHESION',
     'Dùng it, this, they, which sao cho rõ đang chỉ về danh từ nào; nếu lặp danh từ thì thay bằng từ thay thế.',
     '{}'::varchar[]),
    ('REPETITION', 'Lặp lại một từ hoặc cụm từ', 'VOCABULARY', 1, 'V55_PARAPHRASE_WORDS',
     'Đổi cách diễn đạt khi cùng một từ lặp nhiều lần, ví dụ visit, go to, use the library.',
     '{}'::varchar[]),
    ('INFORMAL_REGISTER', 'Văn phong thân mật trong bài học thuật', 'VOCABULARY', 1, 'W80_ACADEMIC_STYLE',
     'Dùng văn phong học thuật: không viết tắt (don''t), tránh kids, a lot of, stuff.',
     '{}'::varchar[]),
    ('WRITING_SPELLING', 'Sai chính tả trong bài viết', 'VOCABULARY', 1, NULL,
     'Viết lại từ và kiểm tra từng chữ cái, đặc biệt các chữ cái gần giống nhau.',
     '{}'::varchar[]),
    ('WRITING_PLURAL_FORM', 'Sai dạng số ít hoặc số nhiều trong bài viết', 'GRAMMAR', 1, 'G55_ARTICLES',
     'Danh từ đếm được số nhiều cần thêm -s: five age groups, 9 visits.',
     '{}'::varchar[])
) AS v(code, name, category, severity, kp_code, fix_hint, tasks)
LEFT JOIN knowledge_points kp ON kp.code = v.kp_code
ON CONFLICT (code) DO NOTHING;

-- The model definitions of every Writing code that is active after this migration.
UPDATE error_types et
SET model_definition = v.definition, updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('ARTICLE', 'Use when an article (a, an or the) is missing, extra or wrong for its noun: the for a known or unique noun, a or an for a noun mentioned for the first time. Do not use for a wrong plural (WRITING_PLURAL_FORM) or a wrong word. Example: "in library" should be "in the library".'),
    ('COLLOCATION', 'Use when each word is right but the pair does not go together naturally, for example "make a research" should be "do research". Do not use when the meaning itself is wrong (WORD_CHOICE) or a preposition is wrong (PREPOSITION).'),
    ('COMPARISON', 'Use when a comparison is built wrongly: a double form such as "more wider", "then" where "than" is needed, or a missing second element. Do not use for a wrong tense or a wrong word meaning. Example: "more wider" should be "wider".'),
    ('PASSIVE_VOICE', 'Use when a passive structure is wrong: be is missing or in the wrong form, or the past participle is missing or replaced by the base verb. Example: "The form is collect" should be "The form is collected". Do not use when the verb is active and correct.'),
    ('SUBJECT_VERB_AGREEMENT', 'Use when the verb does not agree in number with its real subject, for example a singular verb after a plural subject. Find the real subject first; a nearby noun does not decide the verb. Do not use for a missing verb (MISSING_VERB) or a wrong verb form (VERB_FORM).'),
    ('SENTENCE_FRAGMENT', 'Use when a subordinate clause or a phrase is punctuated as a full sentence, for example "Because the price rose." Do not use when the sentence has no finite verb at all (MISSING_VERB).'),
    ('MISSING_VERB', 'Use when a clause has no finite verb, for example "They only 6 visits." Do not use for a subordinate clause punctuated alone (SENTENCE_FRAGMENT) or for a verb in the wrong form (VERB_FORM).'),
    ('VERB_FORM', 'Use when a verb has the wrong form for its position: a base form is needed after to or a modal, -ing after a helper, the correct past form of an irregular verb (grew, rose, not growed or rised), or am, is or are before a main verb. Do not use for a wrong tense (TENSE), wrong agreement (SUBJECT_VERB_AGREEMENT) or a wrong passive (PASSIVE_VOICE). Example: "can goes" should be "can go".'),
    ('TENSE', 'Use when the tense does not match the time shown in the sentence, for example a present form for a finished year in the past. Example: "In 2000 it is 9" should be "In 2000 it was 9". Do not use for a perfect tense (PERFECT_TENSE) or a wrong verb form (VERB_FORM).'),
    ('PERFECT_TENSE', 'Use when a present perfect or past perfect is missing or wrong, or a perfect tense is used with a finished past time. Example: "Since 2000 it increase" should be "Since 2000 it has increased". Do not use for a simple past verb.'),
    ('RUN_ON_SENTENCE', 'Use when two independent clauses are joined only by a comma or by nothing. Example: "Visits rose, the number was high." should be "Visits rose, and the number was high." Do not use for a correct compound sentence with a conjunction or a semicolon.'),
    ('WORD_FORM', 'Use when a word has the wrong part of speech for its position, for example an adjective where an adverb is needed. Example: "a naturally process" should be "a natural process". Do not use when the word class is right and the meaning is wrong (WORD_CHOICE).'),
    ('WORD_CHOICE', 'Use when a word has the wrong meaning or nuance for the context. Example: "The chart explains the number of visits" should be "The chart shows the number of visits". Do not use when the word is right but does not combine naturally (COLLOCATION).'),
    ('PREPOSITION', 'Use when a preposition is missing, extra or wrong after a verb, noun or adjective. Examples: "despite of" should be "despite"; "the number visits" should be "the number of visits". Do not use for an unnatural word pair (COLLOCATION) or a wrong word (WORD_CHOICE).'),
    ('LINKER_MISUSE', 'Use when a linking word shows the wrong relation between two ideas, for example "Therefore" for a contrast. Example: "Visits rose. Therefore, the number fell." should be "Visits rose. However, the number fell." Do not use when the link is right but repeated (REPETITION).'),
    ('NO_CLEAR_POSITION', 'Use when the question asks for the writer''s opinion and no position is stated, or the stated position changes later in the essay. Do not use when the question does not ask for an opinion.'),
    ('UNDERDEVELOPED_IDEA', 'Use when a main idea has no reason or example, or the example is too general to show a person, place or figure. Do not use when the idea is off topic (OFF_TOPIC) or the example does not fit the idea (T2_IRRELEVANT_EXAMPLE).'),
    ('T2_IRRELEVANT_EXAMPLE', 'Use when an example does not fit the idea it supports, for example an example about wild animals for an idea about online study. Do not use when the example is only general (UNDERDEVELOPED_IDEA).'),
    ('T2_PARTIAL_ANSWER', 'Use when a part of the question is not answered, or only partly answered, for example when one of two views is missing. Do not use when the answer is present but weak (UNDERDEVELOPED_IDEA).'),
    ('OFF_TOPIC', 'Use when a paragraph or sentence does not answer the question asked. Do not use when the content is on topic but thin (UNDERDEVELOPED_IDEA) or an example is off the idea (T2_IRRELEVANT_EXAMPLE).'),
    ('T1_NO_OVERVIEW', 'Use when the report has no overview of the main trends or features, or the overview states a wrong main trend. Do not use for one wrong figure (T1_DATA_MISREPORTED).'),
    ('T1_DATA_MISREPORTED', 'Use when a figure, comparison, trend, step of a process or change between two maps contradicts the chart facts. The suggestion must be correct in both grammar and data. Example: "Visits rose from 9 to 6" should be "Visits fell from 9 to 6". Do not use for a vague trend word (DATA_LANGUAGE).'),
    ('T1_IRRELEVANT_CONTENT', 'Use when the report gives an opinion, a reason, a recommendation or a prediction that the chart does not show. Give an empty suggestion, because the sentence should be removed. Example: "because libraries became more popular" as a reason for the figures. Do not use for a wrong figure (T1_DATA_MISREPORTED).'),
    ('DATA_LANGUAGE', 'Use when a trend or quantity is described with the wrong word or structure, for example "increase very big" where "increase sharply" is meant. Do not use when the figure itself is wrong (T1_DATA_MISREPORTED).'),
    ('REFERENCE', 'Use when it, this, they or which has no clear noun to refer to, or a noun is repeated where a reference word would read better. Example: "Visits rose and it was high." Do not use for a wrong word (WORD_CHOICE).'),
    ('REPETITION', 'Use when the same word or phrase is repeated several times where the essay could vary it, for example "visit" in every sentence. Give one entry for the repeated phrase, not one per occurrence. Do not use for a deliberate repeat of a key term.'),
    ('INFORMAL_REGISTER', 'Use when a word or form is too informal for an academic report: contractions, kids, a lot of, stuff, get. Do not use for a grammar or spelling error.'),
    ('WRITING_SPELLING', 'Use when a word is misspelled but the intended word is clear. Example: "enviroment" should be "environment". Do not use when the wrong spelling is another real word (WORD_CHOICE).'),
    ('WRITING_PLURAL_FORM', 'Use when a countable noun has the wrong number: a plural is needed after a number or many, or a singular after each or every. Examples: "five age group" should be "five age groups"; "9 visit" should be "9 visits". Do not use for a missing article (ARTICLE).'),
    ('GRAMMAR_OTHER', 'Use only for a grammar error that no other GRAMMAR code fits. Do not use when a specific GRAMMAR code fits.'),
    ('VOCABULARY_OTHER', 'Use only for a vocabulary error that no other VOCABULARY code fits. Do not use when a specific VOCABULARY code fits.'),
    ('COHERENCE_OTHER', 'Use only for an organisation or linking error that no other COHERENCE code fits. Do not use when a specific COHERENCE code fits.'),
    ('TASK_OTHER', 'Use only for a breach of a task requirement that no other TASK code fits. Do not use when a specific TASK code fits.')
) AS v(code, definition)
WHERE et.code = v.code;

-- Refuse to finish unless the Writing codes are complete: ten new active codes, a definition on every active Writing
-- code, the shared codes no longer on Writing, and each code that points at a practice point points at an active one.
DO $$
BEGIN
    IF (SELECT count(*) FROM error_types WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills)
            AND code IN ('T1_IRRELEVANT_CONTENT', 'DATA_LANGUAGE', 'MISSING_VERB', 'PERFECT_TENSE', 'RUN_ON_SENTENCE',
                         'REFERENCE', 'REPETITION', 'INFORMAL_REGISTER', 'WRITING_SPELLING', 'WRITING_PLURAL_FORM')) <> 10 THEN
        RAISE EXCEPTION 'the ten new Writing error codes must exist, be active and be Writing codes';
    END IF;
    IF EXISTS (SELECT 1 FROM error_types WHERE code IN ('SPELLING', 'PLURAL_FORM') AND 'WRITING' = ANY(skills)) THEN
        RAISE EXCEPTION 'SPELLING and PLURAL_FORM must not be Writing codes';
    END IF;
    IF EXISTS (SELECT 1 FROM error_types WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills)
            AND (model_definition IS NULL OR btrim(model_definition) = '')) THEN
        RAISE EXCEPTION 'every active Writing error code needs a model definition';
    END IF;
    IF EXISTS (
        SELECT 1 FROM error_types et
        JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
        LEFT JOIN topics t ON t.id = parent.topic_id
        LEFT JOIN courses c ON c.id = t.course_id
        WHERE et.code IN ('T1_IRRELEVANT_CONTENT', 'DATA_LANGUAGE', 'MISSING_VERB', 'PERFECT_TENSE', 'RUN_ON_SENTENCE',
                          'REFERENCE', 'REPETITION', 'INFORMAL_REGISTER', 'WRITING_SPELLING', 'WRITING_PLURAL_FORM',
                          'OFF_TOPIC')
          AND (parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE')
    ) THEN
        RAISE EXCEPTION 'a code changed by this migration points at a point that is not reachable by a learner';
    END IF;
END $$;
