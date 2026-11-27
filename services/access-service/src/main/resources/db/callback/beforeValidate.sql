-- Flyway runs this before every validation. Some migrations got a new version number after developer databases had
-- already run them under the old one; their content did not change, so only the recorded number is moved. Does nothing
-- on a new database or once the history is right. When a migration that may already have run is renumbered, add its
-- new version and file name here.
DO $callback$
DECLARE
    moved INTEGER;
BEGIN
    IF to_regclass('flyway_schema_history') IS NULL THEN
        RETURN;
    END IF;
    -- Repeat because one migration can only move after another has left the number it needs.
    LOOP
        UPDATE flyway_schema_history AS history
        SET version = renumbered.version,
            script = 'V' || renumbered.version || '__' || renumbered.name || '.sql'
        FROM (VALUES
            ('14', 'key_distribution_audit_and_issue_requests'),
            ('16', 'enforce_human_grading_credits'),
            ('17', 'one_active_subscription_per_user')
        ) AS renumbered(version, name)
        WHERE history.type = 'SQL'
          AND history.description = replace(renumbered.name, '_', ' ')
          AND history.version <> renumbered.version
          AND NOT EXISTS (SELECT 1 FROM flyway_schema_history AS taken WHERE taken.version = renumbered.version);
        GET DIAGNOSTICS moved = ROW_COUNT;
        EXIT WHEN moved = 0;
    END LOOP;
END
$callback$;
