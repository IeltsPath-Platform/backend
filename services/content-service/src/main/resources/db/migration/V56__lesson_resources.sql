-- Lesson resources: a lesson gets a Markdown summary and links to its slides and course documents. Files live
-- outside the platform (Drive, CDN, Google Slides), so only their URLs are stored. Topics, blocks and practice sets
-- are unchanged; lessons only gain a nullable column.

ALTER TABLE lessons ADD COLUMN summary_markdown TEXT;

CREATE TABLE lesson_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('SLIDE', 'DOCUMENT')),
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    url TEXT NOT NULL CHECK (url ~* '^https?://'),
    sort_order INT NOT NULL CHECK (sort_order >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Leads with lesson_id, so it also serves the "resources of a lesson" lookup.
    CONSTRAINT uq_lesson_resources_order UNIQUE (lesson_id, resource_type, sort_order)
);
