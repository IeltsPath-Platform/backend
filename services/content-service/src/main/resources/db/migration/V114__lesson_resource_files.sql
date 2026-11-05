-- A lesson document can be a file uploaded to storage instead of a link. The row then keeps the storage key in
-- file_key and no url; the API signs a short-lived download link for it. Slides stay links. Existing rows keep their
-- url and a null file_key.
ALTER TABLE lesson_resources ADD COLUMN file_key VARCHAR(300);
ALTER TABLE lesson_resources ALTER COLUMN url DROP NOT NULL;
ALTER TABLE lesson_resources DROP CONSTRAINT IF EXISTS lesson_resources_url_check;
ALTER TABLE lesson_resources ADD CONSTRAINT ck_lesson_resources_target CHECK (
    (url IS NOT NULL AND file_key IS NULL AND url ~* '^https?://')
    OR (url IS NULL AND file_key IS NOT NULL AND resource_type = 'DOCUMENT' AND file_key LIKE 'lesson-document/%'));
