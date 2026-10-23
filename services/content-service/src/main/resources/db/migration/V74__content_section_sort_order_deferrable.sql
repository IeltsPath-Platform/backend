-- Section order is rewritten as one set, so the unique order must be checkable at commit, not per row.
-- Ordinary inserts and updates still check immediately; only a transaction that asks for it defers the check.
ALTER TABLE content_sections DROP CONSTRAINT uq_content_sections_sort_order;
ALTER TABLE content_sections ADD CONSTRAINT uq_content_sections_sort_order
    UNIQUE (package_version_id, sort_order) DEFERRABLE INITIALLY IMMEDIATE;
