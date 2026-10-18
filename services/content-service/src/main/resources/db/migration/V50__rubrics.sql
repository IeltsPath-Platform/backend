CREATE TABLE rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL,
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1', 'TASK_2')),
    rubric_version INTEGER NOT NULL CHECK (rubric_version > 0),
    status VARCHAR(10) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    source_note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rubrics_code_version UNIQUE (code, rubric_version),
    CONSTRAINT uq_rubrics_task_code_version UNIQUE (task, code, rubric_version)
);

CREATE UNIQUE INDEX uq_rubrics_one_active_per_task ON rubrics (task) WHERE status = 'ACTIVE';

CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    code VARCHAR(8) NOT NULL CHECK (code IN ('TR', 'TA', 'CC', 'LR', 'GRA')),
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order > 0),
    CONSTRAINT uq_rubric_criteria_code UNIQUE (rubric_id, code),
    CONSTRAINT uq_rubric_criteria_order UNIQUE (rubric_id, sort_order)
);

CREATE TABLE rubric_band_descriptors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    criterion_id UUID NOT NULL REFERENCES rubric_criteria(id) ON DELETE CASCADE,
    band NUMERIC(2,1) NOT NULL CHECK (band >= 0 AND band <= 9 AND band * 2 = trunc(band * 2)),
    descriptor TEXT NOT NULL,
    CONSTRAINT uq_rubric_descriptor_band UNIQUE (criterion_id, band)
);

CREATE TABLE rubric_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    band NUMERIC(2,1) NOT NULL CHECK (band >= 0 AND band <= 9 AND band * 2 = trunc(band * 2)),
    prompt TEXT NOT NULL,
    essay TEXT NOT NULL,
    commentary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The descriptors below are original paraphrases of the public IELTS Writing band descriptors.
-- They are learning and calibration notes, not reproduced official wording.
INSERT INTO rubrics (id, code, task, rubric_version, status, source_note) VALUES
    ('39a00000-0000-4000-8000-000000000001', 'IELTS_WRITING_TASK_1', 'TASK_1', 1, 'ACTIVE',
     'Original paraphrase for calibration, based on the public IELTS Writing band descriptors (ielts.org): https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf. Draft requires review by an IELTS-qualified examiner; this AI-authored draft has not yet been reviewed.'),
    ('39a00000-0000-4000-8000-000000000002', 'IELTS_WRITING_TASK_2', 'TASK_2', 1, 'ACTIVE',
     'Original paraphrase for calibration, based on the public IELTS Writing band descriptors (ielts.org): https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf. Draft requires review by an IELTS-qualified examiner; this AI-authored draft has not yet been reviewed.');

WITH criteria AS (
    SELECT '39a00000-0000-4000-8000-000000000001'::uuid AS rubric_id, v.code, v.name, v.description, v.sort_order
    FROM (VALUES
        ('TA','Task Achievement','How fully the response selects, reports and supports the key information in the visual or prompt.',1),
        ('CC','Coherence and Cohesion','How clearly ideas are ordered and connected, with reference and linking that help the reader.',2),
        ('LR','Lexical Resource','Range, precision and appropriateness of vocabulary, including control of collocation and word formation.',3),
        ('GRA','Grammatical Range and Accuracy','Range of sentence forms and control of grammar, punctuation and sentence boundaries.',4)
    ) v(code,name,description,sort_order)
    UNION ALL
    SELECT '39a00000-0000-4000-8000-000000000002'::uuid, v.code, v.name, v.description, v.sort_order
    FROM (VALUES
        ('TR','Task Response','How directly the response answers every part of the question and develops relevant ideas with support.',1),
        ('CC','Coherence and Cohesion','How clearly ideas are ordered and connected, with reference and linking that help the reader.',2),
        ('LR','Lexical Resource','Range, precision and appropriateness of vocabulary, including control of collocation and word formation.',3),
        ('GRA','Grammatical Range and Accuracy','Range of sentence forms and control of grammar, punctuation and sentence boundaries.',4)
    ) v(code,name,description,sort_order)
)
INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
SELECT rubric_id, code, name, description, sort_order FROM criteria;

WITH bands(band) AS (VALUES (4.0),(4.5),(5.0),(5.5),(6.0),(6.5),(7.0),(7.5),(8.0),(8.5),(9.0))
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT c.id, b.band,
       CASE b.band
         WHEN 4.0 THEN 'Performance is limited: important features may be absent or confused, support is weak, and recurring language problems can make meaning difficult to follow.'
         WHEN 4.5 THEN 'Performance remains limited, though some key features are recognizable; support is thin and frequent language problems interrupt meaning.'
         WHEN 5.0 THEN 'Performance is basic and uneven: the main requirement is recognizable, but selection or development has gaps and language control is inconsistent.'
         WHEN 5.5 THEN 'Performance is developing: the main requirement is mostly addressed, but development remains uneven and language errors are noticeable.'
         WHEN 6.0 THEN 'Performance is generally effective: the main requirement is addressed with relevant support, though some ideas or language choices remain underdeveloped.'
         WHEN 6.5 THEN 'Performance is mostly effective and relevant, with clearer development and control, though occasional gaps prevent consistently strong execution.'
         WHEN 7.0 THEN 'Performance is clear and well organized: the response develops its main points with relevant detail and varied language, with occasional imprecision.'
         WHEN 7.5 THEN 'Performance is strong and well controlled: ideas are developed clearly and language is varied, with only minor imprecision or lapses.'
         WHEN 8.0 THEN 'Performance is strong and precise: the task is handled fully, ideas are well supported and language is flexible with only rare slips.'
         WHEN 8.5 THEN 'Performance is very strong and precise: the response is fully developed and language is flexible and accurate, with only occasional minor slips.'
         ELSE 'Performance is consistently precise and complete: ideas are fully developed and language control is natural, accurate and flexible.'
       END || ' ' || CASE c.code
         WHEN 'TA' THEN 'For this visual report, select the most important data, describe the overall pattern and make accurate comparisons.'
         WHEN 'TR' THEN 'For this essay, answer every part of the question, present a clear position and extend relevant reasons or examples.'
         WHEN 'CC' THEN 'Order ideas so the progression is easy to follow and use linking and reference without obscuring meaning.'
         WHEN 'LR' THEN 'Choose vocabulary that is sufficiently varied, accurate and natural for the ideas being expressed.'
         ELSE 'Use a suitable range of sentence forms and maintain control of grammar, punctuation and sentence boundaries.'
       END
FROM rubric_criteria c CROSS JOIN bands b;

INSERT INTO rubric_examples (rubric_id, band, prompt, essay, commentary) VALUES
('39a00000-0000-4000-8000-000000000001', 6,
 'The chart compares the share of household waste recycled in three towns in 2010 and 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.',
 'The chart shows that recycling increased in all three towns between 2010 and 2020. Brookfield had the highest figure in both years, rising from 42% to 68%. Lakeside grew from 30% to 55%, while Hilltown recorded the smallest increase, from 25% to 39%. Overall, the gap between Brookfield and the other towns became slightly wider.',
 'Original calibration sample. It identifies the overall trend and key comparisons, but provides limited detail about intermediate values and uses a small range of structures.'),
('39a00000-0000-4000-8000-000000000002', 6,
 'Some people think public libraries are no longer necessary because information is available online. To what extent do you agree or disagree?',
 'Although people can find a great deal of information online, I do not think public libraries have become unnecessary. They provide free access to books, computers and quiet study areas, which is important for people who cannot afford these resources at home. Libraries also offer help from trained staff, especially for children and older users who may find digital information difficult to assess. However, libraries should continue to provide online services as well as printed books. In conclusion, the internet has changed the role of libraries, but these institutions still serve an important purpose.',
 'Original calibration sample. It gives a clear position and relevant reasons, though the supporting examples and vocabulary remain fairly general.');
