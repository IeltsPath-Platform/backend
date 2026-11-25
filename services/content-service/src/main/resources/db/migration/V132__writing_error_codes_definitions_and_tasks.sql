-- Writing error codes for the grader (product owner decisions of 2026-11-13). Each Writing code gets a definition the
-- model reads (model_definition, English), the task(s) it applies to (writing_tasks; an empty array means no task, so
-- non-Writing codes stay empty and a Writing code always lists its task), and whether the model may choose it
-- (model_selectable). fix_hint stays the Vietnamese advice shown to the learner.
--
-- Codes are never renamed or re-pointed here except where listed. SPELLING and PLURAL_FORM are tagged automatically on
-- Listening and Reading gap fills, so they only lose the WRITING skill. Writing uses WRITING_SPELLING and NOUN_PLURAL.
-- TENSE and LINKER_MISUSE keep their points: their trap points and practice sets depend on those points.
-- The definitions and names are not yet reviewed by an IELTS examiner; a later migration can change them.
--
-- Points for the model-selectable codes: NO_CLEAR_POSITION, T1_NO_OVERVIEW and T2_PARTIAL_ANSWER are still evidence codes,
-- but the grader must not choose them once the backend builds them from taskChecks (a later change).

-- Snapshots for the final checks: these values must be the same after this migration.
CREATE TEMP TABLE writing_code_before AS
    SELECT code, category, knowledge_point_id, trap_knowledge_point_id, fix_hint
    FROM error_types
    WHERE code IN ('SPELLING', 'PLURAL_FORM', 'TENSE', 'SENTENCE_FRAGMENT', 'LINKER_MISUSE', 'ARTICLE', 'SUBJECT_VERB_AGREEMENT');

ALTER TABLE error_types
    ADD COLUMN model_definition TEXT,
    ADD COLUMN writing_tasks VARCHAR(10)[] NOT NULL DEFAULT '{}',
    ADD COLUMN model_selectable BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE error_types ADD CONSTRAINT chk_error_types_writing_tasks
    CHECK (writing_tasks <@ ARRAY['TASK_1', 'TASK_2']::varchar[]);

-- Existing codes.
UPDATE error_types
SET skills = array_remove(skills, 'WRITING'::varchar), updated_at = CURRENT_TIMESTAMP
WHERE code IN ('SPELLING', 'PLURAL_FORM');

UPDATE error_types
SET category = 'TASK', model_selectable = false, updated_at = CURRENT_TIMESTAMP
WHERE code = 'NO_CLEAR_POSITION';

UPDATE error_types
SET category = 'VOCABULARY', updated_at = CURRENT_TIMESTAMP
WHERE code = 'PREPOSITION';

UPDATE error_types
SET model_selectable = false, updated_at = CURRENT_TIMESTAMP
WHERE code IN ('T1_NO_OVERVIEW', 'T2_PARTIAL_ANSWER');

UPDATE error_types
SET fix_hint = 'Kiểm tra dạng động từ: sau trợ động từ và modal dùng nguyên mẫu (can go), không đặt am/is/are trước động từ thường (is have → has), dùng đúng dạng quá khứ của động từ bất quy tắc (grew, rose).',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'VERB_FORM';

-- Task of each Writing code that already exists.
UPDATE error_types SET writing_tasks = ARRAY['TASK_1']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code IN ('T1_DATA_MISREPORTED', 'T1_NO_OVERVIEW');

UPDATE error_types SET writing_tasks = ARRAY['TASK_2']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code IN ('NO_CLEAR_POSITION', 'OFF_TOPIC', 'T2_IRRELEVANT_EXAMPLE', 'T2_PARTIAL_ANSWER', 'UNDERDEVELOPED_IDEA');

UPDATE error_types SET writing_tasks = ARRAY['TASK_1', 'TASK_2']::varchar[], updated_at = CURRENT_TIMESTAMP
WHERE code IN ('ARTICLE', 'COLLOCATION', 'COMPARISON', 'LINKER_MISUSE', 'PASSIVE_VOICE', 'PREPOSITION',
               'SENTENCE_FRAGMENT', 'SUBJECT_VERB_AGREEMENT', 'TENSE', 'VERB_FORM', 'WORD_CHOICE', 'WORD_FORM',
               'GRAMMAR_OTHER', 'VOCABULARY_OTHER', 'COHERENCE_OTHER', 'TASK_OTHER');

-- New codes: approved and Writing-only, with no trap point yet (a trap point needs published practice sets with trap
-- questions, which are not written). WRITING_SPELLING has no knowledge point, so it creates no evidence.
INSERT INTO error_types (code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, skills,
                         fallback, writing_tasks)
SELECT v.code, v.name, v.category, v.severity, kp.id, v.fix_hint, 'ACTIVE', 'APPROVED', ARRAY['WRITING']::varchar[],
       false, v.tasks
FROM (VALUES
    ('T1_IRRELEVANT_CONTENT', 'Nêu ý kiến hoặc lý do không có trong biểu đồ', 'TASK', 2, 'W60_T1_KEY_FEATURES',
     'Task 1 chỉ mô tả những gì biểu đồ cho thấy; bỏ ý kiến, lời khuyên, nguyên nhân hay dự đoán không có trong đề.',
     ARRAY['TASK_1']::varchar[]),
    ('DATA_LANGUAGE', 'Diễn đạt xu hướng hoặc số lượng chưa đúng', 'VOCABULARY', 1, 'V60_DATA_TREND_WORDS',
     'Dùng cụm chuẩn để mô tả số liệu: rose sharply, fell slightly, reached a peak of, made 9 visits.',
     ARRAY['TASK_1']::varchar[]),
    ('MISSING_VERB', 'Câu thiếu động từ chính', 'GRAMMAR', 2, 'G55_BASIC_SENTENCE_PATTERNS',
     'Mỗi mệnh đề cần một động từ chia theo chủ ngữ; kiểm tra có thiếu be hay động từ chính không.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('PAST_SIMPLE_TENSE', 'Sai thì quá khứ đơn', 'GRAMMAR', 1, 'G40_PAST_SIMPLE',
     'Mốc thời gian đã qua (in 2000, last year) dùng quá khứ đơn: was, became, rose.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('RUN_ON_SENTENCE', 'Nối hai câu bằng dấu phẩy', 'GRAMMAR', 1, 'W50_COMPOUND_SENTENCES',
     'Hai mệnh đề độc lập phải tách bằng dấu chấm hoặc nối bằng từ nối (and, but, while, whereas).',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('NOUN_PLURAL', 'Sai số ít hoặc số nhiều của danh từ', 'GRAMMAR', 1, 'G55_ARTICLES',
     'Danh từ đếm được sau số lớn hơn một cần -s: five age groups, 9 visits; people đã là số nhiều.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('REFERENCE', 'Từ thay thế không rõ chỉ cái gì', 'COHERENCE', 1, 'W60_PARAGRAPH_COHESION',
     'Dùng it, this, they chỉ khi người đọc biết rõ từ đó thay cho danh từ nào; nếu không rõ, nhắc lại danh từ.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('REPETITION', 'Lặp từ nhiều lần', 'VOCABULARY', 1, 'V55_PARAPHRASE_WORDS',
     'Chuẩn bị từ thay thế cho từ hay lặp: visits / trips to the library, number / figure.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('INFORMAL_REGISTER', 'Dùng từ thân mật trong bài học thuật', 'VOCABULARY', 1, 'W80_ACADEMIC_STYLE',
     'Bài IELTS Writing dùng văn phong trang trọng: không viết tắt (do not), tránh kids, a lot of, stuff.',
     ARRAY['TASK_1', 'TASK_2']::varchar[]),
    ('WRITING_SPELLING', 'Sai chính tả', 'VOCABULARY', 1, NULL,
     'Kiểm tra lại các từ hay viết sai: library, increase, government, environment.',
     ARRAY['TASK_1', 'TASK_2']::varchar[])
) AS v(code, name, category, severity, kp_code, fix_hint, tasks)
LEFT JOIN knowledge_points kp ON kp.code = v.kp_code
ON CONFLICT (code) DO NOTHING;

-- Model definitions of every Writing code that is active after this migration (product owner text, unchanged).
UPDATE error_types et
SET model_definition = v.definition, updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('ARTICLE', 'Use for a missing, extra or wrong article (a, an, the): "The number of the visits rose" -> "The number of visits rose". Not for a missing plural -s (NOUN_PLURAL).'),
    ('COLLOCATION', 'Use when the meaning is right but the words do not naturally go together: "make a research" -> "do research". Not for a wrong meaning (WORD_CHOICE), a wrong word class (WORD_FORM), or a Task 1 trend or quantity phrase (DATA_LANGUAGE).'),
    ('COMPARISON', 'Use for a wrongly built comparative or superlative: "more higher", "then" for "than", "the most high". Not for a comparison that contradicts the chart (T1_DATA_MISREPORTED).'),
    ('LINKER_MISUSE', 'Use when a linking word shows the wrong relation between ideas ("However" for an addition, "because" for a result) or is used where none fits. Not for an unclear it/this/they (REFERENCE).'),
    ('NO_CLEAR_POSITION', 'Task 2, only when the question asks for an opinion: the essay never states a clear position, or later contradicts it.'),
    ('OFF_TOPIC', 'Task 2: a sentence or paragraph that does not answer the question that was asked. Not for a relevant idea with a poor example (T2_IRRELEVANT_EXAMPLE).'),
    ('PASSIVE_VOICE', 'Use for a wrongly built passive ("is collect", "was locate", "are remove") or a passive where the active is needed. Not for other verb forms (VERB_FORM).'),
    ('PREPOSITION', 'Use for a wrong, missing or extra preposition: "increase of 5%" -> "increase by 5%", "discuss about". Not for a whole unnatural phrase (COLLOCATION).'),
    ('SENTENCE_FRAGMENT', 'Use for a dependent clause or phrase written as a full sentence: "Because the price rose." / "Which shows a rise.". Not for a clause with no verb (MISSING_VERB) or two sentences joined by a comma (RUN_ON_SENTENCE).'),
    ('SUBJECT_VERB_AGREEMENT', 'Use when only the agreement is wrong: "it have", "they was", "the number of visits are". If the verb form is also wrong ("they was go"), give VERB_FORM as a second entry.'),
    ('T1_DATA_MISREPORTED', 'Task 1: a figure, trend, comparison, process step or map change that contradicts <chart_facts>. The suggestion states what the chart shows. Grammar errors in the same words get their own entries.'),
    ('T1_NO_OVERVIEW', 'Task 1: the essay has no overview of the main trends or features.'),
    ('T2_IRRELEVANT_EXAMPLE', 'Task 2: an example that does not support the point of its paragraph. A relevant but too general example is UNDERDEVELOPED_IDEA.'),
    ('T2_PARTIAL_ANSWER', 'Task 2: one part of the question is not answered, or is only mentioned without development.'),
    ('TENSE', 'Use when the verb form is correct but the tense does not fit the time or meaning, such as present perfect with a finished time ("has increased in 2000") or present continuous for a general fact. A finished past time that needs the past simple uses PAST_SIMPLE_TENSE.'),
    ('UNDERDEVELOPED_IDEA', 'Task 2: a main idea with no reason or example, or with an example too general to support it. Quote the topic sentence.'),
    ('VERB_FORM', 'Use for a wrong verb form that no more specific code covers: be + base verb ("is have", "was go"), modal + -s or to ("can goes"), wrong -ing or to-infinitive ("enjoy to read"), wrong irregular past ("growed"). Not agreement only (SUBJECT_VERB_AGREEMENT), tense choice (TENSE, PAST_SIMPLE_TENSE), passive (PASSIVE_VOICE) or a missing verb (MISSING_VERB).'),
    ('WORD_CHOICE', 'Use when a word has the wrong meaning in context: "The table talking about" -> "The table shows". Not for an unnatural pairing (COLLOCATION) or a wrong word class (WORD_FORM).'),
    ('WORD_FORM', 'Use when the word class is wrong: "a convenience way" -> "a convenient way", "grow rapid" -> "grow rapidly". Not for a wrong meaning (WORD_CHOICE).'),
    ('GRAMMAR_OTHER', 'Fallback: a grammar error that no listed grammar code covers. Never use it when a specific code fits.'),
    ('VOCABULARY_OTHER', 'Fallback: a vocabulary error that no listed vocabulary code covers. Never use it when a specific code fits.'),
    ('COHERENCE_OTHER', 'Fallback: an organisation or cohesion error that no listed coherence code covers. Never use it when a specific code fits.'),
    ('TASK_OTHER', 'Fallback: a task problem that no listed task code covers. Never use it when a specific code fits.'),
    ('T1_IRRELEVANT_CONTENT', 'Task 1: a personal opinion, recommendation, cause or prediction that the visual does not show ("because people like reading", "everyone should go to the library"). The suggestion is an empty string.'),
    ('DATA_LANGUAGE', 'Task 1: a wrong or unnatural expression for a trend, change or quantity when the figure itself is right ("increase very big", "the number was go down"). If the figure is also wrong, add a T1_DATA_MISREPORTED entry.'),
    ('MISSING_VERB', 'A clause has no finite verb, usually a missing be or main verb: "they only 6 visit" -> "they made only 6 visits".'),
    ('PAST_SIMPLE_TENSE', 'A finished past time needs the past simple and another tense or the base form is used: "in 2020 it become 4" -> "became", "in 2000 the number is 9" -> "was".'),
    ('RUN_ON_SENTENCE', 'Two independent clauses joined only by a comma or with no linking word: "The number rose, it reached 12 in 2020". Not for a linking word with the wrong meaning (LINKER_MISUSE).'),
    ('NOUN_PLURAL', 'A countable noun that needs the plural ("five age group", "9 visit", "every years" -> "every year") or a wrong plural ("peoples"). Not for subject-verb agreement.'),
    ('REFERENCE', 'A reference word (it, this, they, that, which) whose referent is unclear or wrong, or a noun repeated where a reference word is clearly needed. Not for a linking word (LINKER_MISUSE).'),
    ('REPETITION', 'The same word or phrase repeated several times where an alternative is expected. Give one entry that quotes a repeated occurrence. Not for a single wrong word (WORD_CHOICE).'),
    ('INFORMAL_REGISTER', 'Informal words or forms in academic writing: contractions (''don''t''), "kids", "a lot of", "stuff".'),
    ('WRITING_SPELLING', 'A misspelled word ("libary", "increace"). Not for a wrong word class (WORD_FORM) or a missing plural -s (NOUN_PLURAL).')
) AS v(code, definition)
WHERE et.code = v.code;

-- Refuse to finish unless the Writing codes are complete and the shared codes and trap points are untouched.
DO $$
BEGIN
    IF (SELECT count(*) FROM error_types WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills)) <> 33 THEN
        RAISE EXCEPTION 'expected 33 active Writing error codes';
    END IF;
    IF EXISTS (SELECT 1 FROM error_types WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills)
            AND (model_definition IS NULL OR btrim(model_definition) = '' OR cardinality(writing_tasks) = 0)) THEN
        RAISE EXCEPTION 'every active Writing error code needs a model definition and at least one Writing task';
    END IF;
    IF (SELECT count(*) FROM error_types WHERE code IN ('T1_IRRELEVANT_CONTENT', 'DATA_LANGUAGE', 'MISSING_VERB',
            'PAST_SIMPLE_TENSE', 'RUN_ON_SENTENCE', 'NOUN_PLURAL', 'REFERENCE', 'REPETITION', 'INFORMAL_REGISTER',
            'WRITING_SPELLING') AND status = 'ACTIVE' AND 'WRITING' = ANY(skills)) <> 10 THEN
        RAISE EXCEPTION 'the ten new Writing error codes must exist, be active and be Writing codes';
    END IF;
    IF EXISTS (SELECT 1 FROM error_types WHERE code IN ('SPELLING', 'PLURAL_FORM') AND 'WRITING' = ANY(skills)) THEN
        RAISE EXCEPTION 'SPELLING and PLURAL_FORM must not be Writing codes';
    END IF;
    IF EXISTS (
        SELECT 1 FROM error_types et
        JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
        LEFT JOIN topics t ON t.id = parent.topic_id
        LEFT JOIN courses c ON c.id = t.course_id
        WHERE et.code IN ('T1_IRRELEVANT_CONTENT', 'DATA_LANGUAGE', 'MISSING_VERB', 'PAST_SIMPLE_TENSE', 'RUN_ON_SENTENCE',
                          'NOUN_PLURAL', 'REFERENCE', 'REPETITION', 'INFORMAL_REGISTER')
          AND (parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE')
    ) THEN
        RAISE EXCEPTION 'a new Writing error code points at a point that is not reachable by a learner';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_code_before b
        JOIN error_types et ON et.code = b.code
        WHERE b.code IN ('SPELLING', 'PLURAL_FORM')
          AND (et.category IS DISTINCT FROM b.category OR et.knowledge_point_id IS DISTINCT FROM b.knowledge_point_id
               OR et.fix_hint IS DISTINCT FROM b.fix_hint)
    ) THEN
        RAISE EXCEPTION 'SPELLING and PLURAL_FORM must keep their category, point and hint';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_code_before b
        JOIN error_types et ON et.code = b.code
        WHERE b.code IN ('TENSE', 'SENTENCE_FRAGMENT', 'LINKER_MISUSE', 'ARTICLE', 'SUBJECT_VERB_AGREEMENT', 'PLURAL_FORM')
          AND (et.knowledge_point_id IS DISTINCT FROM b.knowledge_point_id
               OR et.trap_knowledge_point_id IS DISTINCT FROM b.trap_knowledge_point_id)
    ) THEN
        RAISE EXCEPTION 'a point or trap point that must not change was changed';
    END IF;
END $$;
