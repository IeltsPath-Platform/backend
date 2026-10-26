-- Each IELTS Writing rubric criterion is evidence for its own knowledge point, so mastery can tell which criterion a
-- learner is weak in. The points live in the active DEMO_WRITING topic so they reach every learner's catalog. The
-- mapping is keyed by task and criterion code, not by rubric version: published rubrics are immutable, and a retired
-- version that an activated prompt still uses keeps its knowledge points.

INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status) VALUES
    ('3c000000-0000-4000-8000-020000000001', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_TA', 'Hoàn thành yêu cầu Task 1 (TA)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Viết đoạn tổng quan, chọn và so sánh đúng các đặc điểm chính của biểu đồ, đủ số từ yêu cầu.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000002', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_TR', 'Trả lời đúng đề Task 2 (TR)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Trả lời đủ mọi phần của đề, giữ quan điểm rõ và nhất quán, phát triển ý bằng lý do và ví dụ.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000003', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_CC', 'Mạch lạc và liên kết (CC)', 'STRATEGY', 'PROCEDURE', 'WRITING',
     'Chia đoạn hợp lý, sắp ý theo trình tự rõ ràng và nối câu bằng từ nối, đại từ tham chiếu đúng chức năng.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000004', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_LR', 'Vốn từ vựng (LR)', 'VOCABULARY', 'MEMORY', 'WRITING',
     'Dùng từ đa dạng, đúng nghĩa, đúng kết hợp từ và chính tả; diễn đạt lại thay vì lặp từ của đề.', 'ACTIVE'),
    ('3c000000-0000-4000-8000-020000000005', (SELECT id FROM topics WHERE code = 'DEMO_WRITING'),
     'DEMO_WRITING_CRITERION_GRA', 'Ngữ pháp đa dạng và chính xác (GRA)', 'GRAMMAR', 'CONCEPT', 'WRITING',
     'Kết hợp câu đơn và câu phức, dùng đúng thì, hòa hợp chủ vị và dấu câu, ít lỗi làm sai nghĩa.', 'ACTIVE');

CREATE TABLE writing_criterion_knowledge_points (
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1', 'TASK_2')),
    criterion_code VARCHAR(8) NOT NULL,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    PRIMARY KEY (task, criterion_code),
    CONSTRAINT uq_writing_criterion_kp UNIQUE (task, knowledge_point_id),
    CONSTRAINT chk_writing_criterion_code CHECK (
        (task = 'TASK_1' AND criterion_code IN ('TA', 'CC', 'LR', 'GRA'))
        OR (task = 'TASK_2' AND criterion_code IN ('TR', 'CC', 'LR', 'GRA')))
);

INSERT INTO writing_criterion_knowledge_points (task, criterion_code, knowledge_point_id)
SELECT mapping.task, mapping.criterion_code, kp.id
FROM (VALUES
    ('TASK_1', 'TA', 'DEMO_WRITING_CRITERION_TA'),
    ('TASK_1', 'CC', 'DEMO_WRITING_CRITERION_CC'),
    ('TASK_1', 'LR', 'DEMO_WRITING_CRITERION_LR'),
    ('TASK_1', 'GRA', 'DEMO_WRITING_CRITERION_GRA'),
    ('TASK_2', 'TR', 'DEMO_WRITING_CRITERION_TR'),
    ('TASK_2', 'CC', 'DEMO_WRITING_CRITERION_CC'),
    ('TASK_2', 'LR', 'DEMO_WRITING_CRITERION_LR'),
    ('TASK_2', 'GRA', 'DEMO_WRITING_CRITERION_GRA')
) AS mapping(task, criterion_code, kp_code)
JOIN knowledge_points kp ON kp.code = mapping.kp_code;

DO $$
BEGIN
    IF (SELECT count(*) FROM writing_criterion_knowledge_points) <> 8 THEN
        RAISE EXCEPTION 'writing_criterion_knowledge_points must map all 8 task criteria';
    END IF;
END $$;
