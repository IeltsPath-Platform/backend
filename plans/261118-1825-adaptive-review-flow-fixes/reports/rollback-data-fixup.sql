-- Data fix-up to run BEFORE rolling learning-service back to an image older than V56/V57 (Flyway does not roll back).
--
-- Why: the new code writes values the previous binary cannot read. It maps theory_reason and the probe status with
-- Enum.valueOf, so one review with theory_reason = 'REPEATED_FAIL', or one probe with status = 'SKIPPED', makes every
-- read of that review or diagnosis return 500, and those are blocking reviews.
--
-- Safe to run more than once, and safe to leave in place if the rollback is cancelled: the new code reads the old
-- values too. The columns and CHECK constraints added by V56/V57 stay; the previous binary ignores them.
--
-- What is lost by rolling back:
--   * a review that was moved to the basket after repeated misses (blocking = FALSE, unblocked_at set) stays optional;
--   * the failure baseline (failures_counted_from) is ignored, so the old code counts every failed set again;
--   * the saved probe replies (diagnostic_probes.response) and the conclusion kind are ignored.
--
-- Usage: psql -d learning_db -v ON_ERROR_STOP=1 -f rollback-data-fixup.sql

BEGIN;

-- Counts before, to compare after.
SELECT 'theory_reason REPEATED_FAIL' AS what, count(*) AS rows FROM review_items WHERE theory_reason = 'REPEATED_FAIL'
UNION ALL
SELECT 'trigger_kind PRIORITY/DECAYED', count(*) FROM review_items WHERE trigger_kind IN ('PRIORITY', 'DECAYED')
UNION ALL
SELECT 'probe status SKIPPED', count(*) FROM diagnostic_probes WHERE status = 'SKIPPED';

-- A review that went back to the theory step for the third miss read the same theory as the second one.
UPDATE review_items SET theory_reason = 'SECOND_FAIL' WHERE theory_reason = 'REPEATED_FAIL';

-- Basket-started reviews of a weak or decayed point: the old code only knows the tiers it could start from.
UPDATE review_items SET trigger_kind = 'CONSOLIDATE' WHERE trigger_kind IN ('PRIORITY', 'DECAYED');

-- A skipped probe was never shown to the learner and has no package; the old code treats NO_SET as "unknown".
UPDATE diagnostic_probes SET status = 'NO_SET' WHERE status = 'SKIPPED';

-- Counts after: every figure above must now be 0.
SELECT 'theory_reason REPEATED_FAIL' AS what, count(*) AS rows FROM review_items WHERE theory_reason = 'REPEATED_FAIL'
UNION ALL
SELECT 'trigger_kind PRIORITY/DECAYED', count(*) FROM review_items WHERE trigger_kind IN ('PRIORITY', 'DECAYED')
UNION ALL
SELECT 'probe status SKIPPED', count(*) FROM diagnostic_probes WHERE status = 'SKIPPED';

COMMIT;
