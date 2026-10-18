ALTER TABLE content_package_versions
    ADD COLUMN publish_at TIMESTAMPTZ,
    ADD COLUMN last_publish_error VARCHAR(100);

CREATE INDEX idx_package_versions_due_publish
    ON content_package_versions (publish_at, id)
    WHERE status = 'DRAFT' AND publish_at IS NOT NULL;
