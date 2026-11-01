-- Published lesson counts for each course skill; zero rows identify skills with no sellable lessons.
WITH course_skills AS (
    SELECT c.id AS course_id, c.code AS course_code, c.band_level, skill.name AS skill
    FROM courses c
    CROSS JOIN (VALUES ('LISTENING'), ('READING'), ('WRITING'), ('SPEAKING')) AS skill(name)
)
SELECT cs.course_code, cs.band_level, cs.skill, count(DISTINCT l.id) AS published_lesson_count,
       greatest(count(DISTINCT l.id) - 3, 0) AS lessons_beyond_free_preview
FROM course_skills cs
LEFT JOIN topics t ON t.course_id = cs.course_id AND t.skill = cs.skill
LEFT JOIN lessons l ON l.topic_id = t.id AND l.status = 'PUBLISHED'
GROUP BY cs.course_id, cs.course_code, cs.band_level, cs.skill
ORDER BY cs.band_level, cs.course_code, cs.skill;

-- Course topics that cannot be grouped into one (course, skill) progression.
SELECT c.code AS course_code, t.code AS topic_code, t.id AS topic_id
FROM topics t
JOIN courses c ON c.id = t.course_id
WHERE t.skill IS NULL
ORDER BY c.band_level, t.sort_order, t.id;

-- Knowledge points whose skill does not match their course topic. ALL remains a topic-neutral label.
SELECT c.code AS course_code, t.code AS topic_code, kp.code AS knowledge_point_code,
       t.skill AS topic_skill, kp.skill AS knowledge_point_skill
FROM knowledge_points kp
JOIN topics t ON t.id = kp.topic_id
JOIN courses c ON c.id = t.course_id
WHERE kp.skill <> 'ALL' AND (t.skill IS NULL OR kp.skill <> t.skill)
ORDER BY c.band_level, t.sort_order, kp.code;

-- Published course lessons whose content teaches multiple skills or disagrees with its topic skill.
WITH taught_skills AS (
    SELECT l.id AS lesson_id, kp.skill
    FROM lessons l
    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
    JOIN lesson_knowledge_points lkp ON lkp.lesson_id = l.id
    JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id
    WHERE l.status = 'PUBLISHED' AND kp.skill <> 'ALL'
    UNION
    SELECT l.id AS lesson_id, kp.skill
    FROM lessons l
    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
    JOIN lesson_blocks b ON b.lesson_id = l.id AND b.block_type = 'TEXT'
    JOIN lesson_block_knowledge_points bkp ON bkp.block_id = b.id
    JOIN knowledge_points kp ON kp.id = bkp.knowledge_point_id
    WHERE l.status = 'PUBLISHED' AND kp.skill <> 'ALL'
    UNION
    SELECT l.id AS lesson_id, q.skill
    FROM lessons l
    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
    JOIN lesson_blocks b ON b.lesson_id = l.id AND b.block_type = 'EXERCISE'
    JOIN lesson_block_questions bq ON bq.block_id = b.id
    JOIN question_versions qv ON qv.id = bq.question_version_id
    JOIN questions q ON q.id = qv.question_id
    WHERE l.status = 'PUBLISHED' AND q.skill <> 'ALL'
), lesson_facts AS (
    SELECT l.id AS lesson_id, c.code AS course_code, t.code AS topic_code, t.skill AS topic_skill,
           l.code AS lesson_code, array_agg(DISTINCT ts.skill ORDER BY ts.skill) AS taught_skills,
           count(DISTINCT ts.skill) AS skill_count
    FROM lessons l
    JOIN topics t ON t.id = l.topic_id
    JOIN courses c ON c.id = t.course_id
    LEFT JOIN taught_skills ts ON ts.lesson_id = l.id
    WHERE l.status = 'PUBLISHED'
    GROUP BY l.id, c.code, t.code, t.skill, l.code
)
SELECT course_code, topic_code, lesson_code, topic_skill, taught_skills
FROM lesson_facts
WHERE skill_count > 1 OR (skill_count > 0 AND (topic_skill IS NULL OR taught_skills[1] <> topic_skill))
ORDER BY course_code, topic_code, lesson_code;
