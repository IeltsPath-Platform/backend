-- Errors an examiner marks inside a learner's work while grading a standalone human review. Each element is one error:
--   {"excerpt": "have rose", "start": 812, "end": 821, "errorCode": "TENSE", "suggestion": "have risen"}
-- errorCode is a code of the Content Service error-type catalog; its group and the criterion it affects follow from the
-- code, so they are not stored here. suggestion is the examiner's own wording for this excerpt. The field names match the
-- corrections the AI grader keeps in grading_jobs.llm_feedback, so a learner report can draw both the same way.
-- The array is written together with band, criteria and feedback when the review is completed, and read back with them.
-- NULL means no errors were marked, which is the value of every review completed before this column existed.
-- Rules on the elements (errorCode is in the catalog, start < end inside the essay, suggestion length) are checked by the
-- application: the catalog lives in another database.
ALTER TABLE human_review_submissions ADD COLUMN annotations JSONB;

ALTER TABLE human_review_submissions ADD CONSTRAINT chk_human_review_annotations_array
    CHECK (annotations IS NULL OR (jsonb_typeof(annotations) = 'array' AND jsonb_array_length(annotations) <= 200));

-- Errors are part of the finished review, so they cannot exist before it is completed.
ALTER TABLE human_review_submissions ADD CONSTRAINT chk_human_review_annotations_completed
    CHECK (annotations IS NULL OR completed_at IS NOT NULL);
