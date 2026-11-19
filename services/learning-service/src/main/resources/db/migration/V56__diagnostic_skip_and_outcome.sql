ALTER TABLE diagnostic_probes DROP CONSTRAINT diagnostic_probes_status_check;
ALTER TABLE diagnostic_probes ADD CONSTRAINT diagnostic_probes_status_check
    CHECK (status IN ('OPEN', 'ANSWERED', 'NO_SET', 'SKIPPED'));

ALTER TABLE diagnostic_sessions ADD COLUMN conclusion_kind VARCHAR(24)
    CHECK (conclusion_kind IN ('GAPS_FOUND', 'ROOT_IS_GAP', 'INCONCLUSIVE', 'NO_PREREQUISITES'));

UPDATE diagnostic_sessions s
SET conclusion_kind = 'NO_PREREQUISITES'
WHERE s.status = 'CONCLUDED'
  AND NOT EXISTS (SELECT 1 FROM diagnostic_probes p WHERE p.session_id = s.id);

ALTER TABLE diagnostic_probes ADD COLUMN response JSONB;
