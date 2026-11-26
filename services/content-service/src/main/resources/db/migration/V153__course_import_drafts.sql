-- A course workbook that an author checked and kept: the rows as read (or as edited since), so the author can open it
-- again, see what is wrong, fix it and import it. The rows are the cells as written; nothing in here is real content.
CREATE TABLE course_import_drafts (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('DRAFT', 'IMPORTED')),
    content JSONB NOT NULL,
    imported_course_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_course_import_drafts_author ON course_import_drafts (author_id, updated_at DESC);
