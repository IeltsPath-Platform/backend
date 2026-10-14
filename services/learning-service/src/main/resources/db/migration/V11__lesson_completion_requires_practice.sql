-- A lesson with required Practice is completed only once its Practice passes. Lessons confirmed before their
-- Practice passed have no pass row and are no longer completed. Lessons without required Practice got a NO_PRACTICE
-- pass row when they were completed, so they keep their completion.
UPDATE lesson_progress lp
SET completed_at = NULL
WHERE lp.completed_at IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM lesson_practice_passes p
                  WHERE p.user_id = lp.user_id AND p.lesson_id = lp.lesson_id);
