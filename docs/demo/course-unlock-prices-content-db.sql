-- Demo data only: sets the Points prices that unlock the skills of the IELTS 5.5 course, so the unlock buttons show on the
-- course page. In production R&D enters these in the admin course drawer; no migration seeds them on purpose.
--
-- How the numbers were chosen (Point cards sell 50 and 100 Points; one AI Writing grading costs 3):
--   * a skill's first 3 lessons are free, so the price follows the lessons behind the paywall, about 5 Points each:
--     Listening 1 lesson, Reading 8, Writing 4. Listening has a floor of 10 and Writing sits higher for the AI grading it
--     brings, so a skill never costs less than it is worth to the learner.
--   * the dearest skill (Reading, 40) fits one 50-Point card and leaves a few gradings; Reading + Listening is exactly 50.
--   * the combo is 20% under the three skills bought apart (75), and one 100-Point card covers it with 40 left for gradings.
--     The API requires it to sit above the dearest skill and below the sum.
--   * Speaking has no content yet and gets no price, so it cannot be bought.
--
--   psql -h localhost -U postgres -d content_db -f backend/docs/demo/course-unlock-prices-content-db.sql
--   (or, with the Compose database: docker cp the file into ieltspath-postgres-1 and run psql -d content_db -f on it)
--
-- Safe to run again: it overwrites the same prices and leaves unlocks that learners already bought untouched.
-- It is a script and not a Flyway migration on purpose: a migration runs in every environment, these prices are for one.
WITH course AS (SELECT id FROM courses WHERE code = 'IELTS_5_5'),
     prices (skill, unlock_points) AS (VALUES ('LISTENING', 10), ('READING', 40), ('WRITING', 25)),
     priced AS (
         INSERT INTO course_skill_prices (course_id, skill, unlock_points)
         SELECT course.id, prices.skill, prices.unlock_points FROM course, prices
         ON CONFLICT (course_id, skill) DO UPDATE SET unlock_points = EXCLUDED.unlock_points
         RETURNING course_id)
UPDATE courses
   SET combo_unlock_points = 60,
       updated_at = CURRENT_TIMESTAMP
 WHERE id IN (SELECT id FROM course);

SELECT c.name, p.skill, p.unlock_points, c.combo_unlock_points
  FROM courses c
  JOIN course_skill_prices p ON p.course_id = c.id
 WHERE c.code = 'IELTS_5_5'
 ORDER BY p.skill;
