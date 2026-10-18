-- Backfill only when the question's course can be derived unambiguously.
-- Placement questions are MEDIUM; course 5.5 questions are EASY; course 6.5 questions are MEDIUM.
-- Unclassified or conflicting rows keep a NULL difficulty and are listed in a NOTICE instead of receiving a guessed value.
-- The migration must not fail on them: content authors may have created versions outside any course or package.

CREATE TEMP TABLE question_difficulty_course_candidates ON COMMIT DROP AS
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
JOIN courses c ON c.id = t.course_id;

CREATE TEMP TABLE question_difficulty_placement_versions ON COMMIT DROP AS
SELECT DISTINCT sq.question_version_id
FROM section_questions sq
JOIN content_sections cs ON cs.id = sq.section_id
JOIN content_package_versions cpv ON cpv.id = cs.package_version_id
JOIN content_packages cp ON cp.id = cpv.package_id
WHERE cp.package_type = 'PLACEMENT_TEST';

UPDATE question_versions qv
SET difficulty = 'MEDIUM'
FROM question_difficulty_placement_versions placement
WHERE qv.id = placement.question_version_id
  AND qv.difficulty IS NULL;

WITH single_course_band AS (
    SELECT question_version_id, MIN(band_level) AS band_level
    FROM question_difficulty_course_candidates
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

DO $$
DECLARE
    unresolved_report TEXT;
BEGIN
    WITH candidate_summary AS (
        SELECT question_version_id,
               COUNT(DISTINCT band_level) AS band_count,
               STRING_AGG(DISTINCT band_level::TEXT, ',' ORDER BY band_level::TEXT) AS bands
        FROM question_difficulty_course_candidates
        GROUP BY question_version_id
    )
    SELECT STRING_AGG(
               FORMAT(
                   'version_id=%s, question_id=%s, version=%s, purpose=%s, candidate_bands=%s, placement=%s',
                   qv.id,
                   qv.question_id,
                   qv.version_number,
                   q.purpose,
                   COALESCE(candidate_summary.bands, 'none'),
                   placement.question_version_id IS NOT NULL
               ),
               E'\n' ORDER BY qv.id
           )
    INTO unresolved_report
    FROM question_versions qv
    JOIN questions q ON q.id = qv.question_id
    LEFT JOIN candidate_summary ON candidate_summary.question_version_id = qv.id
    LEFT JOIN question_difficulty_placement_versions placement ON placement.question_version_id = qv.id
    WHERE qv.difficulty IS NULL;

    IF unresolved_report IS NOT NULL THEN
        RAISE NOTICE 'Difficulty left NULL for these question versions (no single course or placement package); classify them manually:%',
            E'\n' || unresolved_report;
    END IF;
END;
$$;
