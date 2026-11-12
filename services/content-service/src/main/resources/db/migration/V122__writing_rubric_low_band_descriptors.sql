-- The Writing rubrics (V50, version 1) describe bands 4.0 to 9.0 only. An essay weaker than band 4.0 has nothing to match,
-- so the grader returns 4.0 for it even when its own comment calls the language very limited. This migration adds
-- version 2 of both Writing rubrics: every row of the active version is copied unchanged, and descriptors for bands
-- 3.5, 3.0, 2.5, 2.0, 1.5, 1.0 and 0 are added to each criterion. The new wording was reviewed by an IELTS examiner of
-- the project team. Version 1 is retired, not edited or deleted, so a prompt activation that pins it keeps working.
--
-- Rubric rows are never rewritten: the active version is retired and a new one is inserted, as V117 did for Speaking.
-- Knowledge points of the criteria are keyed by task and criterion code, not by rubric version, so they carry over.
-- Where a prompt activation is recorded in assessment (ai_prompt_activations), the grader keeps using the pinned
-- version until the new one is activated there; with no activation it uses the ACTIVE rubric, which is now version 2.
--
-- The admin editor and JdbcRubricRepository.validateForActivation still require exactly bands 4.0 to 9.0 for a Writing
-- rubric, so version 2 cannot be edited and re-activated from the admin UI until that rule is widened.

CREATE TEMP TABLE writing_low_band_sentences (criterion_code VARCHAR(8) NOT NULL, band NUMERIC(2,1) NOT NULL,
    sentence TEXT NOT NULL);
CREATE TEMP TABLE writing_criterion_tails (task VARCHAR(10) NOT NULL, criterion_code VARCHAR(8) NOT NULL,
    tail TEXT NOT NULL);

INSERT INTO writing_low_band_sentences (criterion_code, band, sentence) VALUES
    ('TA', 3.5, 'Performance is very limited: only a few figures are reported, several are wrong or irrelevant, and no clear overall pattern is given.'),
    ('TA', 3.0, 'Performance is poor: the visual is barely addressed, most key figures are missing or misreported, and there is no overview.'),
    ('TA', 2.5, 'Performance is very poor: only isolated, mostly inaccurate references to the visual appear and they are hardly connected to the task.'),
    ('TA', 2.0, 'Performance is minimal: the response is mostly unrelated to the visual, or is a few inaccurate fragments about it.'),
    ('TA', 1.5, 'Performance is almost absent: at most one or two disconnected references to the visual can be found.'),
    ('TA', 1.0, 'The response is irrelevant to the task, apart from at most a word or two that touch the visual.'),
    ('TA', 0.0, 'No assessable response: not attempted, memorised or copied text, or not in English.'),

    ('TR', 3.5, 'Performance is very limited: the question is only partly addressed, no clear position is given, and ideas are few, repeated or barely supported.'),
    ('TR', 3.0, 'Performance is poor: the question is hardly addressed, ideas are irrelevant or unclear, and there is no real support.'),
    ('TR', 2.5, 'Performance is very poor: only isolated ideas touch the topic and they are not developed or linked to a position.'),
    ('TR', 2.0, 'Performance is minimal: the response is mostly off topic, or a few unrelated statements.'),
    ('TR', 1.5, 'Performance is almost absent: at most one or two phrases connect to the topic.'),
    ('TR', 1.0, 'The response is irrelevant to the question, apart from at most a word or two that touch the topic.'),
    ('TR', 0.0, 'No assessable response: not attempted, memorised or copied text, or not in English.'),

    ('CC', 3.5, 'Performance is very limited: ideas are not clearly organised, there is little progression, and linking words are rare, repeated or misused.'),
    ('CC', 3.0, 'Performance is poor: ideas are disconnected, paragraphing is absent or arbitrary, and the reader has to work hard to follow the message.'),
    ('CC', 2.5, 'Performance is very poor: sentences stand alone with almost no linking, and any order of ideas is hard to see.'),
    ('CC', 2.0, 'Performance is minimal: there is almost no organisation, and only a few words show any connection between ideas.'),
    ('CC', 1.5, 'Performance is almost absent: no organisation can be identified beyond individual phrases.'),
    ('CC', 1.0, 'No organisation or cohesion can be identified, because there is almost no connected text.'),
    ('CC', 0.0, 'No assessable response: not attempted, memorised or copied text, or not in English.'),

    ('LR', 3.5, 'Performance is very limited: only basic words are used and often repeated, and frequent errors in word choice, form or spelling distort meaning.'),
    ('LR', 3.0, 'Performance is poor: vocabulary is restricted to simple, familiar words, and errors in word choice or spelling often make the meaning unclear.'),
    ('LR', 2.5, 'Performance is very poor: only a small stock of isolated words and fixed phrases is used, often incorrectly.'),
    ('LR', 2.0, 'Performance is minimal: only a few words are used correctly and meaning is rarely clear.'),
    ('LR', 1.5, 'Performance is almost absent: only isolated words can be understood.'),
    ('LR', 1.0, 'No control of vocabulary can be shown beyond isolated words.'),
    ('LR', 0.0, 'No assessable response: not attempted, memorised or copied text, or not in English.'),

    ('GRA', 3.5, 'Performance is very limited: sentences are attempted but most contain errors, complex forms are rare or wrong, and meaning is often unclear.'),
    ('GRA', 3.0, 'Performance is poor: control of sentence structure is weak, errors dominate and meaning is rarely clear.'),
    ('GRA', 2.5, 'Performance is very poor: only a few short sentences or fragments are correct, and the rest cannot be reliably understood.'),
    ('GRA', 2.0, 'Performance is minimal: sentence structure is almost absent apart from memorised phrases.'),
    ('GRA', 1.5, 'Performance is almost absent: no sentence forms can be identified beyond single phrases.'),
    ('GRA', 1.0, 'No sentence forms can be identified.'),
    ('GRA', 0.0, 'No assessable response: not attempted, memorised or copied text, or not in English.');

-- The closing sentence of each descriptor, identical to the one in version 1.
INSERT INTO writing_criterion_tails (task, criterion_code, tail) VALUES
    ('TASK_1', 'TA', 'For this visual report, select the most important data, describe the overall pattern and make accurate comparisons.'),
    ('TASK_2', 'TR', 'For this essay, answer every part of the question, present a clear position and extend relevant reasons or examples.'),
    ('TASK_1', 'CC', 'Order ideas so the progression is easy to follow and use linking and reference without obscuring meaning.'),
    ('TASK_2', 'CC', 'Order ideas so the progression is easy to follow and use linking and reference without obscuring meaning.'),
    ('TASK_1', 'LR', 'Choose vocabulary that is sufficiently varied, accurate and natural for the ideas being expressed.'),
    ('TASK_2', 'LR', 'Choose vocabulary that is sufficiently varied, accurate and natural for the ideas being expressed.'),
    ('TASK_1', 'GRA', 'Use a suitable range of sentence forms and maintain control of grammar, punctuation and sentence boundaries.'),
    ('TASK_2', 'GRA', 'Use a suitable range of sentence forms and maintain control of grammar, punctuation and sentence boundaries.');

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
            ('IELTS_WRITING_TASK_1', 'TASK_1', '39a00000-0000-4000-8000-000000000003'::uuid),
            ('IELTS_WRITING_TASK_2', 'TASK_2', '39a00000-0000-4000-8000-000000000004'::uuid)
        ) AS item(code, task, new_id)
    LOOP
        SELECT id INTO source_id FROM rubrics WHERE code = spec.code AND status = 'ACTIVE';
        IF source_id IS NULL THEN
            RAISE EXCEPTION 'No ACTIVE rubric % to build version 2 from', spec.code;
        END IF;
        SELECT MAX(rubric_version) + 1 INTO next_version FROM rubrics WHERE code = spec.code;

        UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP WHERE id = source_id;

        INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
        SELECT spec.new_id, code, task, next_version, 'ACTIVE',
               'Version ' || rubric_version || ' plus descriptors for bands 0 to 3.5, so a response weaker than band 4.0 can be graded below 4.0. '
               || 'Original paraphrase for calibration, based on the public IELTS Writing band descriptors (ielts.org): '
               || 'https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf. '
               || 'The added band 0 to 3.5 descriptors were reviewed by an IELTS examiner of the project team.',
               system_prompt
        FROM rubrics WHERE id = source_id;

        INSERT INTO rubric_criteria (id, rubric_id, code, name, description, sort_order)
        SELECT gen_random_uuid(), spec.new_id, code, name, description, sort_order
        FROM rubric_criteria WHERE rubric_id = source_id;

        INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
        SELECT created.id, original.band, original.descriptor
        FROM rubric_criteria previous
        JOIN rubric_band_descriptors original ON original.criterion_id = previous.id
        JOIN rubric_criteria created ON created.rubric_id = spec.new_id AND created.code = previous.code
        WHERE previous.rubric_id = source_id;

        INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
        SELECT created.id, low.band, low.sentence || ' ' || tail.tail
        FROM writing_low_band_sentences low
        JOIN writing_criterion_tails tail ON tail.task = spec.task AND tail.criterion_code = low.criterion_code
        JOIN rubric_criteria created ON created.rubric_id = spec.new_id AND created.code = low.criterion_code;

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
END $$;

DROP TABLE writing_low_band_sentences;
DROP TABLE writing_criterion_tails;
