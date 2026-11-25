ALTER TABLE lesson_writing_submissions
    DROP CONSTRAINT lesson_writing_submissions_check;
ALTER TABLE lesson_writing_submissions
    ADD CONSTRAINT lesson_writing_submissions_check CHECK (
        status <> 'GRADED' OR (result IS NOT NULL AND overall_band IS NOT NULL
                               AND passed IS NOT NULL AND (debit_ledger_entry_id IS NOT NULL OR point_cost = 0)));

ALTER TABLE speaking_submissions DROP CONSTRAINT chk_speaking_submissions_cost;
ALTER TABLE speaking_submissions ADD CONSTRAINT chk_speaking_submissions_cost CHECK (point_cost >= 0);

ALTER TABLE speaking_submissions DROP CONSTRAINT chk_speaking_submission_grade;
ALTER TABLE speaking_submissions ADD CONSTRAINT chk_speaking_submission_grade CHECK (
    status <> 'GRADED' OR (result IS NOT NULL AND (debit_ledger_entry_id IS NOT NULL OR point_cost = 0)));
