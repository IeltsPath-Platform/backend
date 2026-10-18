-- Writing guide of an essay question: how to read the task and how to structure the answer, shown beside the prompt.
-- It is separate from explanation, which holds the sample answer. Null for every existing question version.
ALTER TABLE question_versions ADD COLUMN guide TEXT;
