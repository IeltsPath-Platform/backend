-- Version 3 of both Writing rubrics, as a DRAFT (product owner decisions of 2026-11-13). The version 2 rubric stays ACTIVE,
-- so grading does not change until version 3 is activated by a later migration, after a measured run passes.
--
-- The criterion descriptions carry the scoring question, the ceilings and what the criterion does not judge. The
-- grader prints the name of the criterion before its description, so a description does not repeat it. Every whole band
-- 0 and 1 to 9 has its own descriptor for each criterion; band 0 is one sentence shared by the criteria. A half band
-- (1.5 to 8.5) says only that the response meets every point of the band below and some points of the band above, so
-- the half bands are generated here.
--
-- Two ceiling changes are not in the product owner's text. The "no position" ceiling for Task 2 is 4, the value
-- configured for the same rule in the band-cap settings. Four density ceilings (GRA, LR and TA) were added so that a
-- response with errors in almost every sentence cannot keep a band of 5; they are marked below and need examiner review.
--
-- Rubric rows are never rewritten: a new version is inserted. Examples and the system prompt are copied from the active
-- version. The admin editor and JdbcRubricRepository.validateForActivation still require exactly bands 4.0 to 9.0 for a
-- Writing rubric, so version 3 cannot be activated from the admin UI until that rule is widened.

CREATE TEMP TABLE writing_v3_headers (task VARCHAR(10) NOT NULL, criterion_code VARCHAR(8) NOT NULL,
    description TEXT NOT NULL);
CREATE TEMP TABLE writing_v3_bands (task VARCHAR(10) NOT NULL, criterion_code VARCHAR(8) NOT NULL,
    band NUMERIC(2,1) NOT NULL, descriptor TEXT NOT NULL);

-- 'ANY' rows apply to both tasks: CC, LR and GRA are the same criteria for Task 1 and Task 2.
INSERT INTO writing_v3_headers (task, criterion_code, description) VALUES
    ('TASK_1', 'TA', 'How well the response selects, reports and compares the key features of the visual. Key features are the main trends, the highest and lowest values and the main differences for data; the main stages for a process; the main changes for a map.
Judges: overview, choice of key features, accuracy against <chart_facts>, comparisons, irrelevant content.
Does not judge: grammar, vocabulary or organisation, unless they make the data impossible to understand.
Apply these ceilings before choosing a band:
- Fewer than 150 words: at most 5.
- No overview, or the overview states a wrong main trend: at most 5.
- Most reported figures are wrong: at most 4.
- A personal opinion, a reason or a prediction that the visual does not show: at most 6.
- Most statements about trends or comparisons contradict <chart_facts>, even when the copied figures are right: at most 4.
- The overview states a wrong main trend and most trend or comparison statements are wrong: at most 3.'),
    ('TASK_2', 'TR', 'How fully the response answers every part of the question and develops relevant ideas.
Judges: coverage of each part of the question, position (only when the question asks for one), development and support of ideas, relevance.
Does not judge: grammar, vocabulary or organisation, unless they make the ideas impossible to understand.
Apply these ceilings before choosing a band:
- Fewer than 250 words: at most 5.
- The question asks for an opinion and no position is given: at most 4.
- One part of the question is not answered: at most 5.
- Most of the response is off topic: at most 4.'),
    ('ANY', 'CC', 'How clearly the ideas are ordered and connected.
Judges: logical order and progression, paragraphing, linking words, reference and substitution (it, this, they, the former).
Does not judge: grammar accuracy or word choice, except for linking words and reference.
Apply this ceiling before choosing a band:
- No paragraphing at all: at most 5.'),
    ('ANY', 'LR', 'Range and accuracy of vocabulary.
Judges: range, word choice, collocation, word formation, spelling, register, repetition; in Task 1 also the words used to describe trends and quantities.
Does not judge: sentence structure, verb tense or agreement (these are GRA).
Apply this ceiling before choosing a band:
- Basic words repeated throughout, and word form or plural errors in most sentences: at most 4.'),
    ('ANY', 'GRA', 'Range of sentence structures and grammatical accuracy.
Judges: simple and complex sentences, verb forms, tense, agreement, articles, plurals, sentence boundaries, punctuation.
Does not judge: whether a figure is right (that is TA) or word choice (that is LR).
Apply these ceilings before choosing a band:
- Almost only simple sentences: at most 5.
- Errors in almost every sentence (at most one sentence in five is error-free): at most 4.
- Errors in almost every sentence and several errors make the meaning unclear: at most 3.');

INSERT INTO writing_v3_bands (task, criterion_code, band, descriptor) VALUES
    -- TA (Task 1)
    ('TASK_1', 'TA', 9, 'Every key feature is selected and compared; the overview is clear and accurate; every figure, stage or change is reported accurately; nothing irrelevant is included.'),
    ('TASK_1', 'TA', 8, 'Every key feature is covered and well compared; the overview is clear and accurate; at most a rare minor slip in a detail.'),
    ('TASK_1', 'TA', 7, 'The key features are covered and clearly highlighted; there is a clear, accurate overview; data is selected and compared, though some points could be more fully covered or there is a little unnecessary detail.'),
    ('TASK_1', 'TA', 6, 'The main requirements are met; an overview is attempted but is incomplete or only partly accurate; key features are covered but some details are missing, wrong or irrelevant.'),
    ('TASK_1', 'TA', 5, 'Information is reported mechanically with little selection or comparison; there is no clear overview; several figures are wrong or the response focuses on detail instead of key features.'),
    ('TASK_1', 'TA', 4, 'Several key features are missing or confused; there is no overview and much of the reported data is wrong, irrelevant or repeated.'),
    ('TASK_1', 'TA', 3, 'The task is barely addressed: few key features appear and most of the data is wrong or misunderstood.'),
    ('TASK_1', 'TA', 2, 'The content is barely related to the visual.'),
    ('TASK_1', 'TA', 1, 'The response is unrelated to the visual apart from a few words.'),
    ('TASK_1', 'TA', 0, 'No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.'),
    -- TR (Task 2)
    ('TASK_2', 'TR', 9, 'Every part of the question is fully answered; any required position is clear and consistent throughout; ideas are fully extended and well supported; nothing irrelevant.'),
    ('TASK_2', 'TR', 8, 'Every part is answered well; any required position is clear; ideas are well developed and supported, with at most an occasional lapse.'),
    ('TASK_2', 'TR', 7, 'Every part is answered; any required position is clear throughout; main ideas are extended and supported, though some are over-generalised or lack focus.'),
    ('TASK_2', 'TR', 6, 'Every part is answered, some more fully than others; any required position is relevant but the conclusion may be unclear or repetitive; main ideas are relevant but some are not developed enough.'),
    ('TASK_2', 'TR', 5, 'The question is answered only partly; a required position is unclear or changes; main ideas are few or not developed enough, and some content is irrelevant.'),
    ('TASK_2', 'TR', 4, 'The question is answered minimally or only indirectly; a required position is missing or hard to find; ideas are few, repeated or unsupported.'),
    ('TASK_2', 'TR', 3, 'No part of the question is answered adequately; ideas are few, undeveloped or irrelevant.'),
    ('TASK_2', 'TR', 2, 'The content is barely related to the question.'),
    ('TASK_2', 'TR', 1, 'The response is unrelated to the question apart from a few words.'),
    ('TASK_2', 'TR', 0, 'No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.'),
    -- CC (both tasks)
    ('ANY', 'CC', 9, 'Ideas progress without effort for the reader; linking and reference are natural and never draw attention; paragraphing is skilful.'),
    ('ANY', 'CC', 8, 'Ideas are ordered logically throughout; linking and reference are well managed with rare lapses; paragraphing is appropriate.'),
    ('ANY', 'CC', 7, 'There is clear progression throughout; a range of linking words is used, with some over- or under-use; reference is generally clear; each paragraph has one clear central idea.'),
    ('ANY', 'CC', 6, 'The response is coherent with clear progression in most places; linking works but is sometimes mechanical or faulty; reference is not always clear; paragraphs are used but not always logically.'),
    ('ANY', 'CC', 5, 'There is some organisation but no clear overall progression; linking words are too few, wrong or overused; nouns are repeated because reference is not used; paragraphing is missing or weak.'),
    ('ANY', 'CC', 4, 'Ideas are not arranged in a clear order; only basic linking words are used, often wrongly or repeatedly; paragraphing is missing or confusing.'),
    ('ANY', 'CC', 3, 'Ideas have no logical order; linking words are few and often wrong; the reader must work hard to follow.'),
    ('ANY', 'CC', 2, 'There is very little control of organisation.'),
    ('ANY', 'CC', 1, 'No organised message can be identified.'),
    ('ANY', 'CC', 0, 'No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.'),
    -- LR (both tasks)
    ('ANY', 'LR', 9, 'A wide range of vocabulary is used naturally and precisely; errors are very rare and minor.'),
    ('ANY', 'LR', 8, 'A wide range is used fluently and flexibly, including less common words used skilfully; occasional errors in spelling or word formation.'),
    ('ANY', 'LR', 7, 'The range allows some flexibility and precision; some less common words are used, with awareness of style and collocation; occasional errors in word choice, spelling or word formation.'),
    ('ANY', 'LR', 6, 'The range is adequate for the task; less common words are attempted with some inaccuracy; some errors in spelling or word formation that do not stop communication.'),
    ('ANY', 'LR', 5, 'The range is limited but just enough for the task; words are repeated; noticeable errors in spelling or word formation may cause some difficulty for the reader.'),
    ('ANY', 'LR', 4, 'Only basic vocabulary is used, often repeatedly or inappropriately; frequent errors in word formation and spelling may strain the reader.'),
    ('ANY', 'LR', 3, 'The range is very limited; errors in word choice, form and spelling seriously distort meaning.'),
    ('ANY', 'LR', 2, 'Vocabulary is extremely limited, with almost no control of word formation or spelling.'),
    ('ANY', 'LR', 1, 'Only a few isolated words can be used.'),
    ('ANY', 'LR', 0, 'No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.'),
    -- GRA (both tasks)
    ('ANY', 'GRA', 9, 'A wide range of structures is used flexibly and accurately; errors are very rare and minor.'),
    ('ANY', 'GRA', 8, 'A wide range of structures is used; most sentences are error-free; errors are very occasional.'),
    ('ANY', 'GRA', 7, 'A variety of complex structures is used; error-free sentences are frequent; grammar and punctuation are well controlled with a few errors.'),
    ('ANY', 'GRA', 6, 'Simple and complex sentences are mixed; there are some errors in grammar and punctuation, but they rarely make meaning unclear.'),
    ('ANY', 'GRA', 5, 'The range is limited; complex sentences are attempted but are less accurate than simple ones; grammar and punctuation errors are frequent and may cause difficulty for the reader.'),
    ('ANY', 'GRA', 4, 'The range is very limited and subordinate clauses are rare; some structures are correct but errors are the norm and punctuation is often wrong.'),
    ('ANY', 'GRA', 3, 'Sentences are attempted but errors in grammar and punctuation dominate and distort meaning.'),
    ('ANY', 'GRA', 2, 'There is almost no sentence structure apart from memorised phrases.'),
    ('ANY', 'GRA', 1, 'No rateable language apart from isolated words.'),
    ('ANY', 'GRA', 0, 'No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.');

DO $$
DECLARE
    spec RECORD;
    source_id UUID;
    next_version INTEGER;
    new_criteria INTEGER;
    new_descriptors INTEGER;
BEGIN
    FOR spec IN
        SELECT * FROM (VALUES
            ('IELTS_WRITING_TASK_1', 'TASK_1', '39a00000-0000-4000-8000-000000000005'::uuid),
            ('IELTS_WRITING_TASK_2', 'TASK_2', '39a00000-0000-4000-8000-000000000006'::uuid)
        ) AS item(code, task, new_id)
    LOOP
        SELECT id INTO source_id FROM rubrics WHERE code = spec.code AND status = 'ACTIVE';
        IF source_id IS NULL THEN
            RAISE EXCEPTION 'No ACTIVE rubric % to build version 3 from', spec.code;
        END IF;
        SELECT MAX(rubric_version) + 1 INTO next_version FROM rubrics WHERE code = spec.code;

        INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
        SELECT spec.new_id, code, task, next_version, 'DRAFT',
               'Version ' || rubric_version || ' with criterion descriptions (scope, ceilings, what is not judged), a descriptor for every whole band, '
               || 'and half bands defined from the whole bands. Original paraphrase for calibration, based on the public IELTS Writing band '
               || 'descriptors (ielts.org): https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf. Not official IELTS '
               || 'descriptors. Requires review by an IELTS examiner before activation.',
               system_prompt
        FROM rubrics WHERE id = source_id;

        INSERT INTO rubric_criteria (id, rubric_id, code, name, description, sort_order)
        SELECT gen_random_uuid(), spec.new_id, prev.code, prev.name, h.description, prev.sort_order
        FROM rubric_criteria prev
        JOIN writing_v3_headers h ON h.criterion_code = prev.code AND h.task IN (spec.task, 'ANY')
        WHERE prev.rubric_id = source_id;

        INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
        SELECT created.id, b.band, b.descriptor
        FROM writing_v3_bands b
        JOIN rubric_criteria created ON created.rubric_id = spec.new_id AND created.code = b.criterion_code
        WHERE b.task IN (spec.task, 'ANY');

        INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
        SELECT created.id, n + 0.5,
               'Meets every point of band ' || n || ' and some, but not all, points of band ' || (n + 1) || '.'
        FROM generate_series(1, 8) AS n
        CROSS JOIN rubric_criteria created
        WHERE created.rubric_id = spec.new_id;

        INSERT INTO rubric_examples (rubric_id, band, prompt, essay, commentary)
        SELECT spec.new_id, band, prompt, essay, commentary FROM rubric_examples WHERE rubric_id = source_id;

        SELECT COUNT(*) INTO new_criteria FROM rubric_criteria WHERE rubric_id = spec.new_id;
        SELECT COUNT(*) INTO new_descriptors FROM rubric_band_descriptors d
            JOIN rubric_criteria c ON c.id = d.criterion_id WHERE c.rubric_id = spec.new_id;
        IF new_criteria <> 4 OR new_descriptors <> 4 * 18 THEN
            RAISE EXCEPTION 'Rubric % version % must have 4 criteria with 18 descriptors each, found % criteria and % descriptors',
                spec.code, next_version, new_criteria, new_descriptors;
        END IF;
    END LOOP;

    -- The active versions are unchanged: this migration only adds drafts.
    IF (SELECT count(*) FROM rubrics WHERE code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND status = 'ACTIVE') <> 2 THEN
        RAISE EXCEPTION 'the two Writing rubrics must stay active';
    END IF;
    IF EXISTS (SELECT 1 FROM rubric_criteria c JOIN rubrics r ON r.id = c.rubric_id
               WHERE r.status = 'DRAFT'
                 AND (c.description LIKE 'TA (%' OR c.description LIKE 'TR (%' OR c.description LIKE 'CC (%'
                      OR c.description LIKE 'LR (%' OR c.description LIKE 'GRA (%')) THEN
        RAISE EXCEPTION 'draft criterion descriptions must not repeat the criterion name';
    END IF;
END $$;

DROP TABLE writing_v3_headers;
DROP TABLE writing_v3_bands;
