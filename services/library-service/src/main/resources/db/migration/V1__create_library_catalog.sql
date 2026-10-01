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


CREATE TABLE learning_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    youtube_video_id VARCHAR(32) NOT NULL UNIQUE,
    youtube_url VARCHAR(500) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    duration_seconds INT CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    topic_id UUID,
    level VARCHAR(20) CHECK (level IN ('A2', 'B1', 'B2', 'C1', 'C2')),
    required_feature_key VARCHAR(100),
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
