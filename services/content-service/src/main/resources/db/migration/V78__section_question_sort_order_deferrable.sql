-- Questions in a section are renumbered as a set after a removal or a reordering, so the per-section order must be
-- checkable at commit. Ordinary writes still check immediately.
ALTER TABLE section_questions DROP CONSTRAINT uq_section_questions_sort_order;
ALTER TABLE section_questions ADD CONSTRAINT uq_section_questions_sort_order
    UNIQUE (section_id, sort_order) DEFERRABLE INITIALLY IMMEDIATE;
