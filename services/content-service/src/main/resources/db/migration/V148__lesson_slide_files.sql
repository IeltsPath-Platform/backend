-- Slide links are intentionally removed because lesson slides are now uploaded PDF files.
DELETE FROM lesson_resources WHERE resource_type = 'SLIDE';

ALTER TABLE lesson_resources DROP CONSTRAINT ck_lesson_resources_target;
ALTER TABLE lesson_resources ADD CONSTRAINT ck_lesson_resources_target CHECK (
    (url IS NOT NULL AND file_key IS NULL AND url ~* '^https?://')
    OR (url IS NULL AND file_key IS NOT NULL AND file_key LIKE 'lesson-document/%'));
ALTER TABLE lesson_resources ADD CONSTRAINT ck_lesson_resources_slide_file CHECK (
    resource_type <> 'SLIDE' OR file_key IS NOT NULL);
