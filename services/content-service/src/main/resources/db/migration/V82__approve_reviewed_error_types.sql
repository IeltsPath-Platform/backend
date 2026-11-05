-- An IELTS examiner has reviewed the error taxonomy (names, categories and fix hints). Approved codes are the ones
-- grading, error analysis and the learner-facing feedback accept, and the ones the True/False/Not Given trap
-- auto-fill and the distractor validation require.
UPDATE error_types SET review_status = 'APPROVED', updated_at = CURRENT_TIMESTAMP WHERE review_status = 'DRAFT';
