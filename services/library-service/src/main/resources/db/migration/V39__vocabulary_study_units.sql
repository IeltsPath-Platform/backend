CREATE TABLE vocabulary_study_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES vocabulary_topics(id),
    sort_order INT NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('LEARN','REVIEW')),
    published_revision INT, draft_revision INT, archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (published_revision IS NOT NULL OR draft_revision IS NOT NULL)
);
CREATE INDEX idx_vocab_study_units_topic ON vocabulary_study_units(topic_id,sort_order) WHERE archived_at IS NULL;
CREATE TABLE vocabulary_study_unit_revisions (
    unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id) ON DELETE CASCADE,
    revision INT NOT NULL, title VARCHAR(120) NOT NULL, description VARCHAR(500), introduction_vi TEXT NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('LEARN','REVIEW')),
    exercise_types TEXT[], graded_step_count INT, published_at TIMESTAMPTZ,
    created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(unit_id,revision)
);
CREATE TABLE vocabulary_study_unit_words (
    unit_id UUID NOT NULL, revision INT NOT NULL,
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE RESTRICT, sort_order INT NOT NULL,
    PRIMARY KEY(unit_id,revision,sense_id),
    FOREIGN KEY(unit_id,revision) REFERENCES vocabulary_study_unit_revisions(unit_id,revision) ON DELETE CASCADE
);
CREATE INDEX idx_vocab_study_unit_words_order ON vocabulary_study_unit_words(unit_id,revision,sort_order);
CREATE TABLE vocabulary_study_topic_versions (
    topic_id UUID PRIMARY KEY REFERENCES vocabulary_topics(id), curriculum_version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
