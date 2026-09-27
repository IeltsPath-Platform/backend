-- A read-only copy of the Content reading passage a tutor session was opened on. Copied once at session creation
-- with the learner's own token, so tutor turns never call Content; deleted with the session.
CREATE TABLE session_materials (
    session_id UUID PRIMARY KEY REFERENCES sessions(id) ON DELETE CASCADE,
    material_type VARCHAR(20) NOT NULL CHECK (material_type IN ('READING')),
    section_id UUID NOT NULL,
    package_id UUID NOT NULL,
    title TEXT NOT NULL,
    instructions TEXT NOT NULL DEFAULT '',
    paragraphs JSONB NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL
);

-- Reading questions belong to a passage instead of a knowledge point.
ALTER TABLE notebook_entries ALTER COLUMN knowledge_point_id DROP NOT NULL;
ALTER TABLE notebook_entries ADD COLUMN material_id UUID;
ALTER TABLE notebook_entries ADD COLUMN material_title TEXT NOT NULL DEFAULT '';
ALTER TABLE notebook_entries ADD CONSTRAINT chk_notebook_entries_subject
    CHECK (knowledge_point_id IS NOT NULL OR material_id IS NOT NULL);
CREATE INDEX idx_notebook_entries_user_material ON notebook_entries (user_id, material_id)
    WHERE material_id IS NOT NULL;
