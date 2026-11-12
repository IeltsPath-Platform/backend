-- A missing conclusion is a note about the structure of the essay, not a language error: it is shown from the Task 2
-- checks of the grade (the noConclusion flag) and is not a code the model may attach to a correction or that costs a
-- band by itself. Decided by the product owner on 2026-11-12, after the examiner draft (written by an AI standing in for
-- an examiner) proposed it.
--
-- The code is retired, not deleted: codes are stable identifiers, and a grade stored earlier may still carry it.
UPDATE error_types
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE code = 'T2_NO_CONCLUSION';
