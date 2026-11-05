-- Error-trap points are evidence children; error_types.knowledge_point_id remains the parent.
ALTER TABLE error_types
    ADD COLUMN trap_knowledge_point_id UUID UNIQUE REFERENCES knowledge_points(id);

INSERT INTO knowledge_points (topic_id, code, name, kind, learning_type, skill, description, status)
SELECT parent.topic_id,
       'TRAP_' || et.code,
       et.name,
       parent.kind,
       parent.learning_type,
       parent.skill,
       et.fix_hint,
       'ACTIVE'
FROM error_types et
JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
WHERE et.status = 'ACTIVE'
  AND parent.status = 'ACTIVE';

UPDATE error_types et
SET trap_knowledge_point_id = kp.id
FROM knowledge_points kp,
     knowledge_points parent
WHERE kp.code = 'TRAP_' || et.code
  AND parent.id = et.knowledge_point_id
  AND et.status = 'ACTIVE'
  AND parent.status = 'ACTIVE';

-- Add trap evidence to published answers without changing the correct key.
UPDATE question_versions qv
SET answer_spec = qv.answer_spec || jsonb_build_object(
        'distractors',
        CASE qv.answer_spec->>'correct'
            WHEN 'NOT_GIVEN' THEN jsonb_build_object(
                CASE WHEN qv.options @> '[{"optionKey":"NO"}]' THEN 'NO' ELSE 'FALSE' END,
                'TFNG_NG_AS_FALSE')
            ELSE jsonb_build_object('NOT_GIVEN', 'TFNG_FALSE_AS_NG')
        END)
FROM questions q
WHERE q.id = qv.question_id
  AND q.question_type = 'TRUE_FALSE_NOT_GIVEN'
  AND NOT (qv.answer_spec ? 'distractors')
  AND qv.answer_spec->>'correct' IN ('NOT_GIVEN', 'FALSE', 'NO')
  AND (qv.answer_spec->>'correct' <> 'NOT_GIVEN'
       OR qv.options @> '[{"optionKey":"FALSE"}]'
       OR qv.options @> '[{"optionKey":"NO"}]')
  AND (qv.answer_spec->>'correct' = 'NOT_GIVEN'
       OR qv.options @> '[{"optionKey":"NOT_GIVEN"}]')
  AND (SELECT count(*)
       FROM error_types e
       WHERE e.code IN ('TFNG_NG_AS_FALSE', 'TFNG_FALSE_AS_NG')
         AND e.status = 'ACTIVE') = 2;
