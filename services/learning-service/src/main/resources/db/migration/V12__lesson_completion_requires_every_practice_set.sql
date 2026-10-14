-- A lesson with required Practice is complete only once every required set was submitted and no review is pending.
-- Practice passes stored under the earlier rule (one passed set was enough) no longer mean that, and the lessons they
-- completed show as done too early. Drop those passes and reopen the lessons: their Practice is derived again from the
-- learner's attempts and reviews, and completes the lesson the next time an attempt or review changes it.
-- Lessons without required Practice keep their NO_PRACTICE pass and their completion.
UPDATE lesson_progress lp
SET completed_at = NULL
WHERE lp.completed_at IS NOT NULL
  AND EXISTS (SELECT 1 FROM lesson_practice_passes p
              WHERE p.user_id = lp.user_id AND p.lesson_id = lp.lesson_id AND p.reason <> 'NO_PRACTICE');

DELETE FROM lesson_practice_passes WHERE reason <> 'NO_PRACTICE';
