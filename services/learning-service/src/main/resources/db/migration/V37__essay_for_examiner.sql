-- A learner can send a free-writing essay to an examiner instead of the AI grader. The essay then waits for the examiner
-- (AWAITING_EXAMINER) and costs no points: the examiner's grade is paid with a Premium grading credit, which Access spends.
ALTER TABLE lesson_writing_submissions DROP CONSTRAINT lesson_writing_submissions_status_check;
ALTER TABLE lesson_writing_submissions ADD CONSTRAINT lesson_writing_submissions_status_check
    CHECK (status IN ('GRADING', 'GRADED', 'FAILED', 'PAYMENT_PENDING', 'AWAITING_EXAMINER'));

-- point_cost is what the AI grade costs; an essay graded by an examiner has none.
ALTER TABLE lesson_writing_submissions DROP CONSTRAINT lesson_writing_submissions_point_cost_check;
ALTER TABLE lesson_writing_submissions ADD CONSTRAINT lesson_writing_submissions_point_cost_check CHECK (point_cost >= 0);
