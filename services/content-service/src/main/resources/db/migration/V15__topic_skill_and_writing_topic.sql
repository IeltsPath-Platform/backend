-- A topic that teaches lessons belongs to exactly one IELTS skill, and its lessons inherit it. Topics that only group
-- knowledge points may stay without a skill.
ALTER TABLE topics ADD COLUMN skill VARCHAR(20)
    CONSTRAINT chk_topics_skill CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING'));
CREATE INDEX idx_topics_skill_sort ON topics (skill, sort_order);

UPDATE topics SET skill = 'READING'
WHERE code IN ('DEMO_READING', 'TFNG_SKILLS', 'PREMIUM_MATCHING_INFO', 'PREMIUM_SENTENCE_COMPLETION');
UPDATE topics SET skill = 'LISTENING' WHERE code = 'DEMO_LISTENING';

-- Any other topic takes the one skill shared by all of its active knowledge points; a mix leaves it without a skill.
UPDATE topics t
SET skill = s.skill
FROM (SELECT topic_id, MIN(skill) AS skill
      FROM knowledge_points
      WHERE status = 'ACTIVE' AND skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')
      GROUP BY topic_id
      HAVING COUNT(DISTINCT skill) = 1) s
WHERE t.id = s.topic_id AND t.skill IS NULL;

-- The demo Writing essays sat at the end of the Reading lessons L3 and L4. They move to their own Writing topic with
-- lessons W1 (Task 1) and W2 (Task 2). Block ids stay the same because learners' essay submissions refer to them. The
-- topic has no final test: a learner passes it by completing its lessons.
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM lesson_blocks WHERE id IN (
            '22000000-0000-4000-8000-040000000001', '22000000-0000-4000-8000-040000000002',
            '21000000-0000-4000-8000-040000000001', '21000000-0000-4000-8000-040000000002')) < 4 THEN
        RETURN;
    END IF;

    INSERT INTO topics (id, code, name, sort_order, status, skill) VALUES
        ('25000000-0000-4000-8000-010000000001', 'DEMO_WRITING', 'Writing cơ bản', 950, 'ACTIVE', 'WRITING');

    INSERT INTO lessons (id, topic_id, code, title, sort_order, status) VALUES
        ('25000000-0000-4000-8000-030000000001', '25000000-0000-4000-8000-010000000001', 'W1',
         'Mô tả biểu đồ (Task 1)', 1, 'PUBLISHED'),
        ('25000000-0000-4000-8000-030000000002', '25000000-0000-4000-8000-010000000001', 'W2',
         'Luận quan điểm (Task 2)', 2, 'PUBLISHED');

    -- W1: Task 1 (knowledge point DEMO_READING_W1_CHART).
    UPDATE lesson_blocks
    SET lesson_id = '25000000-0000-4000-8000-030000000001', sort_order = 1, updated_at = CURRENT_TIMESTAMP,
        text_content = 'Writing Task 1 mở bằng câu tổng quan nêu đặc điểm nổi bật nhất của biểu đồ, rồi mới đưa số liệu để chứng minh và so sánh.'
    WHERE id = '22000000-0000-4000-8000-040000000001';
    UPDATE lesson_blocks
    SET lesson_id = '25000000-0000-4000-8000-030000000001', sort_order = 2, updated_at = CURRENT_TIMESTAMP
    WHERE id = '22000000-0000-4000-8000-040000000002';

    -- W2: Task 2 (knowledge point DEMO_READING_W2_OPINION).
    UPDATE lesson_blocks
    SET lesson_id = '25000000-0000-4000-8000-030000000002', sort_order = 1, updated_at = CURRENT_TIMESTAMP,
        text_content = 'Bài luận nêu quan điểm: mở bài nói rõ bạn đồng ý đến mức nào; mỗi đoạn thân bài mở bằng một câu chủ đề rồi chứng minh bằng lý do và ví dụ.'
    WHERE id = '21000000-0000-4000-8000-040000000001';
    UPDATE lesson_blocks
    SET lesson_id = '25000000-0000-4000-8000-030000000002', sort_order = 2, updated_at = CURRENT_TIMESTAMP
    WHERE id = '21000000-0000-4000-8000-040000000002';

    UPDATE knowledge_points
    SET topic_id = '25000000-0000-4000-8000-010000000001', updated_at = CURRENT_TIMESTAMP
    WHERE id IN ('22000000-0000-4000-8000-020000000006', '21000000-0000-4000-8000-020000000007');

    DELETE FROM lesson_knowledge_points
    WHERE (lesson_id = '20000000-0000-4000-8000-030000000003' AND knowledge_point_id = '22000000-0000-4000-8000-020000000006')
       OR (lesson_id = '20000000-0000-4000-8000-030000000004' AND knowledge_point_id = '21000000-0000-4000-8000-020000000007');
    INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id) VALUES
        ('25000000-0000-4000-8000-030000000001', '22000000-0000-4000-8000-020000000006'),
        ('25000000-0000-4000-8000-030000000002', '21000000-0000-4000-8000-020000000007');
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM topics t
               WHERE t.status = 'ACTIVE' AND t.skill IS NULL
                 AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id = t.id AND l.status = 'PUBLISHED')) THEN
        RAISE EXCEPTION 'Every active topic with a published lesson needs a skill';
    END IF;
END $$;
