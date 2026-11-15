-- Makes version 3 of both Writing rubrics (V133) the active version and retires version 2.
--
-- Decided by the product owner on 2026-11-14 to test the new wording and ceilings. The wording and the ceilings have not
-- been reviewed by an IELTS examiner yet, so the source note keeps saying so. Nothing is rewritten: version 2 is retired,
-- not edited or deleted, so a prompt activation in assessment (ai_prompt_activations) that pins it keeps working. With no
-- activation the graders use the ACTIVE rubric, which is now version 3. To go back, write a migration that retires 3 and
-- activates 2 again.
--
-- The one-active-rubric-per-task index forces the order: retire first, then activate.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND rubric_version = 2 AND status = 'ACTIVE';

UPDATE rubrics SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP,
    source_note = source_note || ' Activated on 2026-11-14 by the product owner for testing; the examiner review above is still pending.'
WHERE code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND rubric_version = 3 AND status = 'DRAFT';

DO $$
DECLARE
    active_count integer;
    active_v3 integer;
    retired_v2 integer;
BEGIN
    SELECT count(*), count(*) FILTER (WHERE rubric_version = 3)
    INTO active_count, active_v3
    FROM rubrics WHERE code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND status = 'ACTIVE';
    IF active_count <> 2 OR active_v3 <> 2 THEN
        RAISE EXCEPTION 'expected version 3 ACTIVE for both Writing tasks, found % active rubrics, % of them version 3',
            active_count, active_v3;
    END IF;

    SELECT count(*) INTO retired_v2
    FROM rubrics WHERE code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND rubric_version = 2 AND status = 'RETIRED';
    IF retired_v2 <> 2 THEN
        RAISE EXCEPTION 'expected version 2 RETIRED for both Writing tasks, found %', retired_v2;
    END IF;

    -- Each active criterion still has a descriptor for every whole band and the half bands (18 bands per criterion).
    IF EXISTS (
        SELECT 1 FROM rubrics r JOIN rubric_criteria c ON c.rubric_id = r.id
        LEFT JOIN rubric_band_descriptors d ON d.criterion_id = c.id
        WHERE r.code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND r.status = 'ACTIVE'
        GROUP BY c.id HAVING count(d.id) <> 18) THEN
        RAISE EXCEPTION 'an active Writing criterion does not have 18 band descriptors';
    END IF;
END $$;
