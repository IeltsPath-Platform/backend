-- Speaking shares the versioned rubric authoring model with Writing. The initial rubric is deliberately a DRAFT:
-- its original calibration notes need review by an IELTS-qualified examiner before it can be activated.

ALTER TABLE rubrics ADD COLUMN system_prompt TEXT;
ALTER TABLE rubrics ADD CONSTRAINT chk_rubrics_system_prompt_length
    CHECK (system_prompt IS NULL OR char_length(system_prompt) <= 4000);
ALTER TABLE rubrics DROP CONSTRAINT rubrics_task_check;
ALTER TABLE rubrics ADD CONSTRAINT rubrics_task_check CHECK (task IN ('TASK_1', 'TASK_2', 'SPEAKING'));
ALTER TABLE rubric_criteria DROP CONSTRAINT rubric_criteria_code_check;
ALTER TABLE rubric_criteria ADD CONSTRAINT rubric_criteria_code_check
    CHECK (code IN ('TR', 'TA', 'CC', 'LR', 'GRA', 'FC', 'PR'));

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('5d000000-0000-4000-8000-020000000001', (SELECT id FROM topics WHERE code = 'SPEAKING_FRAMEWORK'),
     'DEMO_SPEAKING_CRITERION_FC', 'Fluency and coherence (FC)', 'STRATEGY', 'PROCEDURE', 'SPEAKING',
     'Sustain speech at a natural pace, organize ideas clearly, and connect them so the listener can follow.', 'ACTIVE'),
    ('5d000000-0000-4000-8000-020000000002', (SELECT id FROM topics WHERE code = 'SPEAKING_FRAMEWORK'),
     'DEMO_SPEAKING_CRITERION_LR', 'Lexical resource (LR)', 'VOCABULARY', 'MEMORY', 'SPEAKING',
     'Use a sufficiently broad, accurate and natural vocabulary to express ideas and repair gaps.', 'ACTIVE'),
    ('5d000000-0000-4000-8000-020000000003', (SELECT id FROM topics WHERE code = 'SPEAKING_FRAMEWORK'),
     'DEMO_SPEAKING_CRITERION_GRA', 'Grammatical range and accuracy (GRA)', 'GRAMMAR', 'CONCEPT', 'SPEAKING',
     'Use varied sentence forms with enough grammatical accuracy to communicate intended meaning.', 'ACTIVE'),
    ('5d000000-0000-4000-8000-020000000004', (SELECT id FROM topics WHERE code = 'SPEAKING_FRAMEWORK'),
     'DEMO_SPEAKING_CRITERION_PR', 'Pronunciation (PR)', 'STRATEGY', 'PROCEDURE', 'SPEAKING',
     'Produce intelligible speech with effective word stress, rhythm, sounds and intonation.', 'ACTIVE');

CREATE TABLE speaking_criterion_knowledge_points (
    criterion_code VARCHAR(8) PRIMARY KEY CHECK (criterion_code IN ('FC', 'LR', 'GRA', 'PR')),
    knowledge_point_id UUID NOT NULL UNIQUE REFERENCES knowledge_points(id)
);

INSERT INTO speaking_criterion_knowledge_points (criterion_code, knowledge_point_id)
SELECT mapping.criterion_code, kp.id
FROM (VALUES
    ('FC', 'DEMO_SPEAKING_CRITERION_FC'),
    ('LR', 'DEMO_SPEAKING_CRITERION_LR'),
    ('GRA', 'DEMO_SPEAKING_CRITERION_GRA'),
    ('PR', 'DEMO_SPEAKING_CRITERION_PR')
) AS mapping(criterion_code, kp_code)
JOIN knowledge_points kp ON kp.code = mapping.kp_code;

DO $$
BEGIN
    IF (SELECT count(*) FROM speaking_criterion_knowledge_points) <> 4 THEN
        RAISE EXCEPTION 'Speaking rubric requires four criterion-to-KP mappings';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM topics WHERE code='SPEAKING_FRAMEWORK' AND status='ACTIVE') THEN
        RAISE EXCEPTION 'SPEAKING_FRAMEWORK must be active before criterion KPs are added';
    END IF;
END $$;

INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
VALUES ('5d000000-0000-4000-8000-030000000001', 'IELTS_SPEAKING', 'SPEAKING', 1, 'DRAFT',
        'Original starter notes for authoring only. Not an official IELTS descriptor set and not reviewed by an IELTS-qualified examiner.',
        'Grade IELTS Speaking practice consistently using the criterion descriptors in this rubric. Base each judgment only on relevant evidence from the response. Keep feedback specific, respectful, and useful to a learner. This starter prompt must be reviewed before activating the rubric.');

WITH criteria(code, name, description, sort_order, kp_code, focus) AS (
    VALUES
        ('FC', 'Fluency and coherence', 'Pace and continuity of speech, logical sequencing, and use of linking and discourse features.', 1,
         'DEMO_SPEAKING_CRITERION_FC', 'maintaining a natural flow and making ideas easy to follow'),
        ('LR', 'Lexical resource', 'Range, precision, appropriateness and flexibility of vocabulary, including paraphrase.', 2,
         'DEMO_SPEAKING_CRITERION_LR', 'choosing precise, varied and natural vocabulary'),
        ('GRA', 'Grammatical range and accuracy', 'Range and flexibility of structures and control of grammatical forms.', 3,
         'DEMO_SPEAKING_CRITERION_GRA', 'using varied structures accurately enough to express intended meaning'),
        ('PR', 'Pronunciation', 'Intelligibility and use of stress, rhythm, sounds and intonation.', 4,
         'DEMO_SPEAKING_CRITERION_PR', 'making speech intelligible through sound, stress, rhythm and intonation')
), inserted AS (
    INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
    SELECT '5d000000-0000-4000-8000-030000000001', code, name, description, sort_order FROM criteria
    RETURNING id, code
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, bands.band,
       CASE WHEN bands.band < 4.0 THEN 'Very limited performance; ' || criteria.focus
              || ' is rarely successful or cannot yet be assessed reliably.'
         ELSE CASE bands.band
         WHEN 4.0 THEN 'Limited control; frequent difficulty with ' || criteria.focus || '.'
         WHEN 4.5 THEN 'Some relevant ability is present, but control is inconsistent and ' || criteria.focus || ' often breaks down.'
         WHEN 5.0 THEN 'Basic ability is evident, though gaps and recurring errors limit ' || criteria.focus || '.'
         WHEN 5.5 THEN 'Developing control; ' || criteria.focus || ' is uneven but meaning is usually recoverable.'
         WHEN 6.0 THEN 'Generally effective performance; ' || criteria.focus || ' works, with noticeable lapses.'
         WHEN 6.5 THEN 'Mostly effective and relevant performance, with occasional gaps in ' || criteria.focus || '.'
         WHEN 7.0 THEN 'Clear and flexible performance; ' || criteria.focus || ' is usually well controlled.'
         WHEN 7.5 THEN 'Strong performance with good range and control; only minor lapses affect ' || criteria.focus || '.'
         WHEN 8.0 THEN 'Very strong performance; ' || criteria.focus || ' is precise and sustained with rare slips.'
         WHEN 8.5 THEN 'Highly developed performance; ' || criteria.focus || ' is consistently effective and flexible.'
         ELSE 'Exceptional and sustained control; ' || criteria.focus || ' is precise, natural and fully effective.'
       END END
FROM inserted
JOIN criteria ON criteria.code = inserted.code
CROSS JOIN generate_series(0, 18) AS steps(step)
CROSS JOIN LATERAL (SELECT steps.step / 2.0 AS band) AS bands;
