-- Lessons: a topic teaches through ordered lessons made of blocks (text, asset, vocabulary, exercise). A topic's
-- final test is a TOPIC_TEST package attached to the topic. Knowledge points no longer carry a band; topics keep it.

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    sort_order INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lessons_topic_sort_order UNIQUE (topic_id, sort_order)
);

CREATE TABLE lesson_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    block_type VARCHAR(20) NOT NULL CHECK (block_type IN ('TEXT', 'ASSET', 'VOCABULARY', 'EXERCISE')),
    text_content TEXT,
    asset_id UUID REFERENCES content_assets(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lesson_blocks_sort_order UNIQUE (lesson_id, sort_order),
    -- TEXT carries prose, ASSET points at an asset; VOCABULARY and EXERCISE keep their items in child tables.
    CONSTRAINT chk_lesson_blocks_payload CHECK (
        (block_type = 'TEXT' AND text_content IS NOT NULL AND asset_id IS NULL) OR
        (block_type = 'ASSET' AND asset_id IS NOT NULL AND text_content IS NULL) OR
        (block_type IN ('VOCABULARY', 'EXERCISE') AND text_content IS NULL AND asset_id IS NULL)
    )
);

-- Vocabulary senses live in Library Service, so the id is a logical reference without a foreign key.
CREATE TABLE lesson_block_vocabulary (
    block_id UUID NOT NULL REFERENCES lesson_blocks(id) ON DELETE CASCADE,
    vocabulary_sense_id UUID NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (block_id, vocabulary_sense_id)
);

CREATE TABLE lesson_block_questions (
    block_id UUID NOT NULL REFERENCES lesson_blocks(id) ON DELETE CASCADE,
    question_version_id UUID NOT NULL REFERENCES question_versions(id),
    sort_order INT NOT NULL,
    PRIMARY KEY (block_id, question_version_id)
);

CREATE TABLE lesson_knowledge_points (
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    PRIMARY KEY (lesson_id, knowledge_point_id)
);

CREATE INDEX idx_lessons_topic ON lessons(topic_id);
CREATE INDEX idx_lesson_blocks_lesson ON lesson_blocks(lesson_id);
CREATE INDEX idx_lesson_block_questions_question_version ON lesson_block_questions(question_version_id);
CREATE INDEX idx_lesson_knowledge_points_kp ON lesson_knowledge_points(knowledge_point_id);

-- A topic's final test belongs to the topic. LESSON stays valid for existing reading packages.
ALTER TABLE content_packages DROP CONSTRAINT IF EXISTS content_packages_package_type_check;
ALTER TABLE content_packages
    ADD CONSTRAINT content_packages_package_type_check
        CHECK (package_type IN ('MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'QUIZ', 'LESSON', 'TOPIC_TEST'));

ALTER TABLE content_packages
    ADD COLUMN topic_id UUID REFERENCES topics(id),
    ADD CONSTRAINT chk_content_packages_topic_test_topic CHECK (package_type <> 'TOPIC_TEST' OR topic_id IS NOT NULL);

CREATE INDEX idx_content_packages_topic ON content_packages(topic_id);

ALTER TABLE knowledge_points
    DROP CONSTRAINT chk_knowledge_points_band_min,
    DROP CONSTRAINT chk_knowledge_points_band_max,
    DROP CONSTRAINT chk_knowledge_points_band_order,
    DROP COLUMN band_min,
    DROP COLUMN band_max;
