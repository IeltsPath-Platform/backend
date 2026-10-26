-- Slides and documents are reordered as one set per type, so the per-lesson order must be checkable at commit.
-- Ordinary writes still check immediately; only a transaction that asks for it defers the check.
ALTER TABLE lesson_resources DROP CONSTRAINT uq_lesson_resources_order;
ALTER TABLE lesson_resources ADD CONSTRAINT uq_lesson_resources_order
    UNIQUE (lesson_id, resource_type, sort_order) DEFERRABLE INITIALLY IMMEDIATE;
