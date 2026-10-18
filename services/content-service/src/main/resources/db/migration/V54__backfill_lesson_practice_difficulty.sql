-- Classify questions that are used only through lesson-owned practice packages.
-- Keep the same mapping and ambiguity rule as V31: 5.5 is EASY, 6.5 is MEDIUM.
WITH course_candidates AS (
    SELECT DISTINCT sq.question_version_id, c.band_level
    FROM section_questions sq
    JOIN content_sections cs ON cs.id = sq.section_id
    JOIN content_package_versions cpv ON cpv.id = cs.package_version_id
    JOIN content_packages cp ON cp.id = cpv.package_id
    JOIN courses c ON c.id = cp.course_id
    WHERE cp.package_type <> 'PLACEMENT_TEST'

    UNION

    SELECT DISTINCT sq.question_version_id, c.band_level
    FROM section_questions sq
    JOIN content_sections cs ON cs.id = sq.section_id
    JOIN content_package_versions cpv ON cpv.id = cs.package_version_id
    JOIN content_packages cp ON cp.id = cpv.package_id
    JOIN topics t ON t.id = cp.topic_id
    JOIN courses c ON c.id = t.course_id
    WHERE cp.package_type <> 'PLACEMENT_TEST'

    UNION

    SELECT DISTINCT qkp.question_version_id, c.band_level
    FROM question_knowledge_points qkp
    JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
    JOIN topics t ON t.id = kp.topic_id
    JOIN courses c ON c.id = t.course_id

    UNION

    SELECT DISTINCT sq.question_version_id, c.band_level
    FROM section_questions sq
    JOIN content_sections cs ON cs.id = sq.section_id
    JOIN content_package_versions cpv ON cpv.id = cs.package_version_id
    JOIN content_packages cp ON cp.id = cpv.package_id
    JOIN lessons l ON l.id = cp.lesson_id
    JOIN topics t ON t.id = l.topic_id
    JOIN courses c ON c.id = t.course_id
    WHERE cp.package_type <> 'PLACEMENT_TEST'
), single_course_band AS (
    SELECT question_version_id, MIN(band_level) AS band_level
    FROM course_candidates
    GROUP BY question_version_id
    HAVING COUNT(DISTINCT band_level) = 1
)
UPDATE question_versions qv
SET difficulty = CASE single_course_band.band_level
    WHEN 5.5 THEN 'EASY'
    WHEN 6.5 THEN 'MEDIUM'
END
FROM single_course_band
WHERE qv.id = single_course_band.question_version_id
  AND qv.difficulty IS NULL
  AND single_course_band.band_level IN (5.5, 6.5);
