-- The examiner's overall band and whether it passed are kept with the applied review, so a practice attempt that counts an essay
-- the examiner graded can take its pass. Reviews applied before this column existed have no result and do not count until
-- they are applied again.
ALTER TABLE human_review_outcomes ADD COLUMN overall_band NUMERIC(3, 1);
ALTER TABLE human_review_outcomes ADD COLUMN passed BOOLEAN;
