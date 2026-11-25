-- Knowledge points for the 24 prompts of the Writing practice tests PT-WRITING-01 to PT-WRITING-12 (part 1 is Task 1,
-- part 2 is Task 2), one or two per prompt, chosen by the type of the prompt.
--
-- Why: the Writing grader now asks the model to judge exactly the knowledge points attached to the question, so a prompt
-- without points gets no judgments. These practice tests belong to no course milestone, and free writing records no
-- mastery evidence; the points only drive the study suggestions shown with the grade.
-- Decided by the product owner on 2026-11-13. Two types have no dedicated point yet: process descriptions use
-- DEMO_WRITING_LINKING (sequence linkers) and map descriptions use W60_T1_KEY_FEATURES.

WITH mapping (pkg_code, part_no, question_version_id, kp_code) AS (VALUES
('PT-WRITING-01', 1, '2d000000-0000-4000-8000-040000001094'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-01', 1, '2d000000-0000-4000-8000-040000001094'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-01', 2, '2d000000-0000-4000-8000-040000001095'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-01', 2, '2d000000-0000-4000-8000-040000001095'::uuid, 'TR_W_OPINION'),
('PT-WRITING-02', 1, '2d000000-0000-4000-8000-040000001096'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-02', 2, '2d000000-0000-4000-8000-040000001097'::uuid, 'W65_DISCUSS_BOTH_VIEWS'),
('PT-WRITING-02', 2, '2d000000-0000-4000-8000-040000001097'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-03', 1, '2d000000-0000-4000-8000-040000001098'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-03', 1, '2d000000-0000-4000-8000-040000001098'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-03', 2, '2d000000-0000-4000-8000-040000001099'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-03', 2, '2d000000-0000-4000-8000-040000001099'::uuid, 'W70_IDEA_DEVELOPMENT'),
('PT-WRITING-04', 1, '2d000000-0000-4000-8000-040000001100'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-04', 1, '2d000000-0000-4000-8000-040000001100'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-04', 2, '2d000000-0000-4000-8000-040000001101'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-04', 2, '2d000000-0000-4000-8000-040000001101'::uuid, 'TR_W_OPINION'),
('PT-WRITING-05', 1, '2d000000-0000-4000-8000-040000001102'::uuid, 'DEMO_WRITING_LINKING'),
('PT-WRITING-05', 2, '2d000000-0000-4000-8000-040000001103'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-05', 2, '2d000000-0000-4000-8000-040000001103'::uuid, 'W70_IDEA_DEVELOPMENT'),
('PT-WRITING-06', 1, '2d000000-0000-4000-8000-040000001104'::uuid, 'W60_T1_KEY_FEATURES'),
('PT-WRITING-06', 2, '2d000000-0000-4000-8000-040000001105'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-06', 2, '2d000000-0000-4000-8000-040000001105'::uuid, 'TR_W_OPINION'),
('PT-WRITING-07', 1, '2d000000-0000-4000-8000-040000002001'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-07', 1, '2d000000-0000-4000-8000-040000002001'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-07', 2, '2d000000-0000-4000-8000-040000002002'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-07', 2, '2d000000-0000-4000-8000-040000002002'::uuid, 'TR_W_OPINION'),
('PT-WRITING-08', 1, '2d000000-0000-4000-8000-040000002003'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-08', 1, '2d000000-0000-4000-8000-040000002003'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-08', 2, '2d000000-0000-4000-8000-040000002004'::uuid, 'W65_DISCUSS_BOTH_VIEWS'),
('PT-WRITING-08', 2, '2d000000-0000-4000-8000-040000002004'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-09', 1, '2d000000-0000-4000-8000-040000002005'::uuid, 'DEMO_WRITING_LINKING'),
('PT-WRITING-09', 2, '2d000000-0000-4000-8000-040000002006'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-09', 2, '2d000000-0000-4000-8000-040000002006'::uuid, 'TR_W_OPINION'),
('PT-WRITING-10', 1, '2d000000-0000-4000-8000-040000002007'::uuid, 'W60_T1_KEY_FEATURES'),
('PT-WRITING-10', 2, '2d000000-0000-4000-8000-040000002008'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-10', 2, '2d000000-0000-4000-8000-040000002008'::uuid, 'W70_IDEA_DEVELOPMENT'),
('PT-WRITING-11', 1, '2d000000-0000-4000-8000-040000002009'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-11', 1, '2d000000-0000-4000-8000-040000002009'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-11', 2, '2d000000-0000-4000-8000-040000002010'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-11', 2, '2d000000-0000-4000-8000-040000002010'::uuid, 'W70_IDEA_DEVELOPMENT'),
('PT-WRITING-12', 1, '2d000000-0000-4000-8000-040000002011'::uuid, 'DEMO_READING_W1_CHART'),
('PT-WRITING-12', 1, '2d000000-0000-4000-8000-040000002011'::uuid, 'DEMO_WRITING_TREND_LANGUAGE'),
('PT-WRITING-12', 2, '2d000000-0000-4000-8000-040000002012'::uuid, 'DEMO_WRITING_TOPIC_SENTENCE'),
('PT-WRITING-12', 2, '2d000000-0000-4000-8000-040000002012'::uuid, 'TR_W_OPINION')
)
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT sq.question_version_id, kp.id, 1.0
FROM mapping m
JOIN content_packages p ON p.code = m.pkg_code
JOIN content_sections cs ON cs.package_version_id = p.current_published_version_id AND cs.part_no = m.part_no
JOIN section_questions sq ON sq.section_id = cs.id
JOIN knowledge_points kp ON kp.code = m.kp_code
WHERE sq.question_version_id = m.question_version_id
ON CONFLICT (question_version_id, knowledge_point_id) DO NOTHING;

DO $$
DECLARE
    prompts integer;
    unlinked integer;
    inactive integer;
    crowded integer;
    expected integer;
    linked integer;
BEGIN
    -- All 24 prompts are found under their expected question version.
    SELECT count(*) INTO prompts
    FROM content_packages p
    JOIN content_sections cs ON cs.package_version_id = p.current_published_version_id
    JOIN section_questions sq ON sq.section_id = cs.id
    WHERE p.code ~ '^PT-WRITING-(0[1-9]|1[0-2])$' AND cs.part_no IN (1, 2)
      AND sq.question_version_id::text ~ '^2d000000-0000-4000-8000-04000000(109[4-9]|110[0-5]|20(0[1-9]|1[0-2]))$';
    IF prompts <> 24 THEN
        RAISE EXCEPTION 'expected 24 Writing practice prompts under their question versions, found %', prompts;
    END IF;

    -- Every one of them has at least one point.
    SELECT count(*) INTO unlinked
    FROM content_packages p
    JOIN content_sections cs ON cs.package_version_id = p.current_published_version_id
    JOIN section_questions sq ON sq.section_id = cs.id
    WHERE p.code ~ '^PT-WRITING-(0[1-9]|1[0-2])$' AND cs.part_no IN (1, 2)
      AND NOT EXISTS (SELECT 1 FROM question_knowledge_points q WHERE q.question_version_id = sq.question_version_id);
    IF unlinked > 0 THEN
        RAISE EXCEPTION '% Writing practice prompts have no knowledge point', unlinked;
    END IF;

    -- Every attached point is an active Writing point under an active topic and course.
    SELECT count(*) INTO inactive
    FROM question_knowledge_points q
    JOIN section_questions sq ON sq.question_version_id = q.question_version_id
    JOIN content_sections cs ON cs.id = sq.section_id
    JOIN content_packages p ON p.current_published_version_id = cs.package_version_id
    JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
    JOIN topics t ON t.id = kp.topic_id
    JOIN courses c ON c.id = t.course_id
    WHERE p.code ~ '^PT-WRITING-(0[1-9]|1[0-2])$'
      AND (kp.status <> 'ACTIVE' OR kp.skill <> 'WRITING' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE');
    IF inactive > 0 THEN
        RAISE EXCEPTION '% attached knowledge points are not active Writing points under an active topic and course', inactive;
    END IF;

    -- The grader asks about at most four points per prompt.
    SELECT count(*) INTO crowded FROM (
        SELECT q.question_version_id
        FROM question_knowledge_points q
        WHERE q.question_version_id::text ~ '^2d000000-0000-4000-8000-04000000(109[4-9]|110[0-5]|20(0[1-9]|1[0-2]))$'
        GROUP BY q.question_version_id HAVING count(*) > 4) too_many;
    IF crowded > 0 THEN
        RAISE EXCEPTION '% Writing practice prompts have more than 4 knowledge points', crowded;
    END IF;

    -- The table above has 43 links.
    expected := 43;
    SELECT count(*) INTO linked FROM question_knowledge_points q
    WHERE q.question_version_id::text ~ '^2d000000-0000-4000-8000-04000000(109[4-9]|110[0-5]|20(0[1-9]|1[0-2]))$';
    IF linked <> expected THEN
        RAISE EXCEPTION 'expected % links on the Writing practice prompts, found %', expected, linked;
    END IF;
END $$;
