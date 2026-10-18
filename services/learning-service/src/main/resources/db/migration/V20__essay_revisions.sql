-- A revised essay links to the graded essay it improves. The first essay has no predecessor and revision_no 1; a learner
-- may revise three times, so revision_no reaches 4 at most (the limit is enforced by the application, the check is a guard).
ALTER TABLE lesson_writing_submissions ADD COLUMN previous_submission_id UUID REFERENCES lesson_writing_submissions (id);
ALTER TABLE lesson_writing_submissions ADD COLUMN revision_no INTEGER NOT NULL DEFAULT 1
    CHECK (revision_no BETWEEN 1 AND 5);
CREATE INDEX idx_lesson_writing_previous ON lesson_writing_submissions (previous_submission_id);
