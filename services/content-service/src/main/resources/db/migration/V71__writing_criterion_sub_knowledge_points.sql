CREATE TABLE writing_criterion_sub_knowledge_points (
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1', 'TASK_2')),
    criterion_code VARCHAR(8) NOT NULL,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    sort_order INTEGER NOT NULL CHECK (sort_order BETWEEN 1 AND 4),
    PRIMARY KEY (task, criterion_code, knowledge_point_id),
    CONSTRAINT uq_writing_sub_kp_task UNIQUE (task, knowledge_point_id),
    CONSTRAINT uq_writing_sub_kp_order UNIQUE (task, criterion_code, sort_order),
    CONSTRAINT fk_writing_sub_kp_criterion FOREIGN KEY (task, criterion_code)
        REFERENCES writing_criterion_knowledge_points (task, criterion_code),
    CONSTRAINT chk_writing_sub_kp_code CHECK (
        (task = 'TASK_1' AND criterion_code IN ('TA', 'CC', 'LR', 'GRA'))
        OR (task = 'TASK_2' AND criterion_code IN ('TR', 'CC', 'LR', 'GRA')))
);

INSERT INTO writing_criterion_sub_knowledge_points (task, criterion_code, knowledge_point_id, sort_order)
SELECT mapping.task, mapping.criterion_code, kp.id, mapping.sort_order
FROM (VALUES
    ('TASK_1', 'CC', 'DEMO_WRITING_LINKING', 1),
    ('TASK_2', 'CC', 'DEMO_WRITING_TOPIC_SENTENCE', 1),
    ('TASK_2', 'CC', 'DEMO_WRITING_LINKING', 2)
) AS mapping(task, criterion_code, kp_code, sort_order)
JOIN knowledge_points kp ON kp.code = mapping.kp_code;

UPDATE error_types
SET knowledge_point_id = (SELECT id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING'),
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'LINKER_MISUSE';

DO $$
BEGIN
    IF (SELECT count(*) FROM writing_criterion_sub_knowledge_points) <> 3 THEN
        RAISE EXCEPTION 'writing_criterion_sub_knowledge_points must hold the 3 seeded mappings';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_criterion_sub_knowledge_points s
        JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
        JOIN topics t ON t.id = kp.topic_id
        JOIN courses c ON c.id = t.course_id
        WHERE kp.status <> 'ACTIVE' OR kp.skill <> 'WRITING' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE') THEN
        RAISE EXCEPTION 'sub knowledge points must be ACTIVE WRITING points of an ACTIVE course';
    END IF;
    IF EXISTS (
        SELECT 1 FROM writing_criterion_sub_knowledge_points s
        JOIN writing_criterion_knowledge_points p ON p.task = s.task AND p.criterion_code = s.criterion_code
        WHERE p.knowledge_point_id = s.knowledge_point_id) THEN
        RAISE EXCEPTION 'a sub knowledge point must differ from its criterion knowledge point';
    END IF;
    IF (SELECT knowledge_point_id FROM error_types WHERE code = 'LINKER_MISUSE')
        IS DISTINCT FROM (SELECT id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING') THEN
        RAISE EXCEPTION 'LINKER_MISUSE must point at DEMO_WRITING_LINKING';
    END IF;
END $$;
