-- Writing grammar error codes name the grammar point they test, and each gets an error-trap point beneath it (as V79 did).
-- The mapping was approved by an IELTS expert on 2026-11-05. Only codes with no point yet are changed, so a point
-- already set by an earlier migration is never overwritten. Codes that no approved point fits stay unmapped.
CREATE TEMP TABLE grammar_error_mapping (
    code TEXT PRIMARY KEY,
    kp_code TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO grammar_error_mapping (code, kp_code) VALUES
    ('ARTICLE', 'G55_ARTICLES'),
    ('SUBJECT_VERB_AGREEMENT', 'G55_SUBJECT_VERB_AGREEMENT'),
    ('PLURAL_FORM', 'G55_ARTICLES'),
    ('TENSE', 'G60_PERFECT_TENSES'),
    ('SENTENCE_FRAGMENT', 'G55_COMPLEX_SENTENCES');

UPDATE error_types et
SET knowledge_point_id = parent.id,
    updated_at = CURRENT_TIMESTAMP
FROM grammar_error_mapping m
JOIN knowledge_points parent ON parent.code = m.kp_code
WHERE et.code = m.code
  AND et.knowledge_point_id IS NULL;

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
JOIN grammar_error_mapping m ON m.code = et.code
JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
WHERE et.status = 'ACTIVE'
  AND parent.status = 'ACTIVE'
  AND et.trap_knowledge_point_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM knowledge_points existing WHERE existing.code = 'TRAP_' || et.code);

UPDATE error_types et
SET trap_knowledge_point_id = trap.id,
    updated_at = CURRENT_TIMESTAMP
FROM grammar_error_mapping m
JOIN knowledge_points trap ON trap.code = 'TRAP_' || m.code
WHERE et.code = m.code
  AND et.trap_knowledge_point_id IS NULL
  AND et.knowledge_point_id IS NOT NULL;

-- Refuse to finish if a mapped point could not be shown to a learner: each parent and trap point must be active under
-- an active topic and course, and each mapped code must have both points.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM error_types et
        JOIN grammar_error_mapping m ON m.code = et.code
        LEFT JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
        LEFT JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id
        LEFT JOIN topics t ON t.id = parent.topic_id
        LEFT JOIN courses c ON c.id = t.course_id
        WHERE parent.id IS NULL OR trap.id IS NULL OR trap.status <> 'ACTIVE' OR parent.status <> 'ACTIVE'
           OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE' OR trap.topic_id <> parent.topic_id
    ) THEN
        RAISE EXCEPTION 'grammar error codes must map to active points with an active trap point under the same topic';
    END IF;
END $$;
