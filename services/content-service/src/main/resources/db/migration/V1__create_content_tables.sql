CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1. CURRICULUM & KNOWLEDGE POINTS
-- ============================================================================

CREATE TABLE topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(50) NOT NULL CHECK (kind IN ('GRAMMAR', 'VOCABULARY', 'STRATEGY', 'PRONUNCIATION')),
    skill VARCHAR(50) CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING', 'ALL')),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_topics_parent_topic_id ON topics(parent_topic_id);
CREATE INDEX idx_knowledge_points_topic_id ON knowledge_points(topic_id);

-- ============================================================================
-- 2. VOCABULARY REPOSITORY
-- ============================================================================

CREATE TABLE vocabulary_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lemma VARCHAR(255) NOT NULL,
    normalized_lemma VARCHAR(255) NOT NULL,
    ipa VARCHAR(255),
    pronunciation_audio_reference TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vocabulary_senses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vocabulary_item_id UUID NOT NULL REFERENCES vocabulary_items(id) ON DELETE CASCADE,
    part_of_speech VARCHAR(50) NOT NULL CHECK (part_of_speech IN ('NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'PREPOSITION', 'CONJUNCTION', 'IDIOM', 'PHRASAL_VERB')),
    english_definition TEXT,
    vietnamese_meaning TEXT NOT NULL,
    example_sentence TEXT NOT NULL,
    image_url TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vocabulary_items_normalized_lemma ON vocabulary_items(normalized_lemma);
CREATE INDEX idx_vocabulary_senses_item_id ON vocabulary_senses(vocabulary_item_id);

-- ============================================================================
-- 3. CONTENT PACKAGES & STRUCTURE
-- ============================================================================

CREATE TABLE content_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    package_type VARCHAR(50) NOT NULL CHECK (package_type IN ('MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'QUIZ', 'LESSON')),
    access_level VARCHAR(50) NOT NULL DEFAULT 'FREE' CHECK (access_level IN ('FREE', 'PREMIUM')),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    current_published_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE content_package_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES content_packages(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    schema_version INT NOT NULL DEFAULT 1,
    published_at TIMESTAMPTZ,
    published_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_content_package_versions UNIQUE (package_id, version_number)
);

CREATE TABLE content_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_version_id UUID NOT NULL REFERENCES content_package_versions(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    skill VARCHAR(50) CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')),
    sort_order INT NOT NULL,
    time_limit_seconds INT,
    instructions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_content_sections_sort_order UNIQUE (package_version_id, sort_order)
);

CREATE INDEX idx_content_package_versions_package ON content_package_versions(package_id);
CREATE INDEX idx_content_sections_package_version ON content_sections(package_version_id);

-- ============================================================================
-- 4. QUESTION BANK
-- ============================================================================

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_type VARCHAR(50) NOT NULL CHECK (question_type IN ('MULTIPLE_CHOICE', 'FILL_IN_BLANK', 'MATCHING', 'TRUE_FALSE_NOT_GIVEN', 'SHORT_ANSWER', 'ESSAY', 'SPEAKING')),
    skill VARCHAR(50) CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')),
    access_level VARCHAR(50) NOT NULL DEFAULT 'FREE' CHECK (access_level IN ('FREE', 'PREMIUM')),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    current_published_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE question_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    stem TEXT NOT NULL,
    options JSONB,
    answer_spec JSONB NOT NULL DEFAULT '{}'::jsonb,
    schema_version INT NOT NULL DEFAULT 1,
    explanation TEXT,
    difficulty VARCHAR(50) CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_versions UNIQUE (question_id, version_number)
);

CREATE TABLE section_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES content_sections(id) ON DELETE CASCADE,
    question_version_id UUID NOT NULL REFERENCES question_versions(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    max_score NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_section_questions_sort_order UNIQUE (section_id, sort_order)
);

CREATE TABLE question_knowledge_points (
    question_version_id UUID NOT NULL REFERENCES question_versions(id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id) ON DELETE CASCADE,
    weight NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    PRIMARY KEY (question_version_id, knowledge_point_id)
);

CREATE INDEX idx_question_versions_question ON question_versions(question_id);
CREATE INDEX idx_section_questions_section ON section_questions(section_id);
CREATE INDEX idx_section_questions_question_version ON section_questions(question_version_id);
CREATE INDEX idx_question_kp_kp_id ON question_knowledge_points(knowledge_point_id);

-- Add circular foreign keys
ALTER TABLE content_packages
    ADD CONSTRAINT fk_content_packages_current_version
    FOREIGN KEY (current_published_version_id) REFERENCES content_package_versions(id) ON DELETE SET NULL;

ALTER TABLE questions
    ADD CONSTRAINT fk_questions_current_version
    FOREIGN KEY (current_published_version_id) REFERENCES question_versions(id) ON DELETE SET NULL;

-- ============================================================================
-- 5. CONTENT ASSETS & ASSET LINKS
-- ============================================================================

CREATE TABLE content_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_type VARCHAR(50) NOT NULL CHECK (asset_type IN ('PASSAGE', 'AUDIO', 'IMAGE', 'VIDEO')),
    text_content TEXT,
    media_reference TEXT,
    duration_seconds INT,
    checksum VARCHAR(128),
    validation_status VARCHAR(50) NOT NULL DEFAULT 'VALID' CHECK (validation_status IN ('PENDING', 'VALID', 'INVALID')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE content_asset_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES content_assets(id) ON DELETE CASCADE,
    section_id UUID REFERENCES content_sections(id) ON DELETE CASCADE,
    question_version_id UUID REFERENCES question_versions(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_asset_link_owner CHECK (
        (section_id IS NOT NULL AND question_version_id IS NULL) OR
        (section_id IS NULL AND question_version_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_content_asset_links_section
    ON content_asset_links(section_id, asset_id)
    WHERE section_id IS NOT NULL;

CREATE UNIQUE INDEX uq_content_asset_links_question_version
    ON content_asset_links(question_version_id, asset_id)
    WHERE question_version_id IS NOT NULL;

CREATE INDEX idx_content_asset_links_asset ON content_asset_links(asset_id);

-- ============================================================================
-- 6. LEARNING VIDEOS
-- ============================================================================

CREATE TABLE learning_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    youtube_video_id VARCHAR(32) NOT NULL UNIQUE,
    youtube_url VARCHAR(500) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    duration_seconds INT CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    level VARCHAR(20) CHECK (level IN ('A2', 'B1', 'B2', 'C1', 'C2')),
    access_level VARCHAR(20) NOT NULL DEFAULT 'FREE' CHECK (access_level IN ('FREE', 'PREMIUM')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE video_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL REFERENCES learning_videos(id) ON DELETE CASCADE,
    sequence_no INT NOT NULL CHECK (sequence_no > 0),
    start_ms INT NOT NULL CHECK (start_ms >= 0),
    end_ms INT NOT NULL CHECK (end_ms > start_ms),
    transcript TEXT NOT NULL,
    translation_vi TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_video_segments UNIQUE (video_id, sequence_no)
);

CREATE TABLE video_segment_lexical_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id UUID NOT NULL REFERENCES video_segments(id) ON DELETE CASCADE,
    vocabulary_sense_id UUID REFERENCES vocabulary_senses(id) ON DELETE SET NULL,
    surface_text VARCHAR(255) NOT NULL,
    start_char INT NOT NULL CHECK (start_char >= 0),
    end_char INT NOT NULL CHECK (end_char > start_char),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_video_segment_lexical_entries UNIQUE (segment_id, start_char, end_char)
);

CREATE INDEX idx_learning_videos_topic ON learning_videos(topic_id);
CREATE INDEX idx_video_segments_video ON video_segments(video_id);
CREATE INDEX idx_video_segment_lexical_entries_segment ON video_segment_lexical_entries(segment_id);
CREATE INDEX idx_video_segment_lexical_entries_sense ON video_segment_lexical_entries(vocabulary_sense_id);

-- ============================================================================
-- 7. TRANSACTIONAL OUTBOX
-- ============================================================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX idx_outbox_events_published_created ON outbox_events(published_at, created_at);

