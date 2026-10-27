-- Demo data only: gives the learner pp3849100@gmail.com (user_db id below) a Premium subscription with four examiner-grading
-- credits left and 30 days from now. Safe to run again: it renews the subscription instead of adding another one, and credits
-- already spent stay recorded (it raises the total, never lowers "used").
--
--   docker cp backend/docs/demo/premium-demo-access-db.sql ieltspath-postgres-1:/tmp/premium-demo.sql
--   docker exec ieltspath-postgres-1 psql -U postgres -d access_db -f /tmp/premium-demo.sql
--
-- It is a script and not a Flyway migration on purpose: a migration runs in every environment, this account exists in one.
WITH learner AS (SELECT 'eda84762-c4e3-4c6b-97d9-b78bb2a4efd9'::uuid AS id),
     premium AS (SELECT id FROM plans WHERE code = 'PREMIUM'),
     renewed AS (
         UPDATE subscriptions s
            SET starts_at = CURRENT_TIMESTAMP,
                ends_at = CURRENT_TIMESTAMP + INTERVAL '30 days',
                human_grading_credits_total = s.human_grading_credits_used + 4,
                cancelled_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                row_version = s.row_version + 1
           FROM learner, premium
          WHERE s.user_id = learner.id AND s.plan_id = premium.id AND s.status = 'ACTIVE'
         RETURNING s.id)
INSERT INTO subscriptions (user_id, plan_id, status, starts_at, ends_at, source_type, human_grading_credits_total)
SELECT learner.id, premium.id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 'ADMIN', 4
  FROM learner, premium
 WHERE NOT EXISTS (SELECT 1 FROM renewed);

SELECT p.code, s.status, s.ends_at, s.human_grading_credits_total - s.human_grading_credits_used AS credits_left
  FROM subscriptions s JOIN plans p ON p.id = s.plan_id
 WHERE s.user_id = 'eda84762-c4e3-4c6b-97d9-b78bb2a4efd9' AND s.status = 'ACTIVE';
