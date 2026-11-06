-- The essay whose grade raised a criterion review. The examiner escalates a waiting Writing criterion review only when it came from
-- another essay; the same essay graded again just confirms it. Rows raised before this column existed keep NULL.
ALTER TABLE review_items ADD COLUMN source_submission_id UUID;
