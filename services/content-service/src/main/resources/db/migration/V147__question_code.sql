ALTER TABLE questions ADD COLUMN code VARCHAR(40);

WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS sequence
    FROM questions
)
UPDATE questions q
SET code = 'Q-' || LPAD(numbered.sequence::text, 6, '0')
FROM numbered
WHERE q.id = numbered.id;

ALTER TABLE questions ALTER COLUMN code SET NOT NULL;
CREATE UNIQUE INDEX uq_questions_code ON questions(code);

INSERT INTO code_settings (code_kind, applied, pattern, start_number, editable)
VALUES ('QUESTION', TRUE, 'Q-{SEQ:6}', 1, FALSE)
ON CONFLICT (code_kind) DO UPDATE
SET applied = TRUE, pattern = EXCLUDED.pattern, editable = FALSE;

INSERT INTO code_counters (code_kind, scope_key, next_number)
SELECT 'QUESTION', '', COUNT(*) + 1
FROM questions
ON CONFLICT (code_kind, scope_key) DO UPDATE
SET next_number = GREATEST(code_counters.next_number, EXCLUDED.next_number);
