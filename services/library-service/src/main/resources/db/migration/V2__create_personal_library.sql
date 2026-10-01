CREATE TABLE video_learning_progress (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL,
    last_position_ms integer NOT NULL DEFAULT 0,
    watched_duration_seconds integer NOT NULL DEFAULT 0,
    progress_percent numeric(5, 2) NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at timestamptz,
    last_watched_at timestamptz,
    completed_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_video_learning_progress_user_video UNIQUE (user_id, video_id),
    CONSTRAINT chk_video_learning_progress_position CHECK (last_position_ms >= 0),
    CONSTRAINT chk_video_learning_progress_watched CHECK (watched_duration_seconds >= 0),
    CONSTRAINT chk_video_learning_progress_percent CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT chk_video_learning_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))
);

CREATE INDEX idx_video_learning_progress_user_updated
    ON video_learning_progress (user_id, updated_at DESC);

CREATE TABLE saved_video_segments (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL,
    segment_id uuid NOT NULL,
    transcript_snapshot text NOT NULL,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_saved_video_segments_user_segment UNIQUE (user_id, segment_id)
);

CREATE INDEX idx_saved_video_segments_user_created
    ON saved_video_segments (user_id, created_at DESC);

CREATE TABLE notes (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    body text NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_notes_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_notes_user_status_updated ON notes (user_id, status, updated_at DESC);

CREATE TABLE flashcard_decks (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    description text,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_flashcard_decks_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE UNIQUE INDEX uq_flashcard_decks_user_name_not_deleted
    ON flashcard_decks (user_id, name)
    WHERE status <> 'DELETED';

CREATE INDEX idx_flashcard_decks_user_status_updated
    ON flashcard_decks (user_id, status, updated_at DESC);

CREATE TABLE flashcards (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    source_type varchar(50) NOT NULL,
    vocabulary_sense_id uuid,
    source_reference_id uuid,
    highlighted_text text,
    front text NOT NULL,
    back text NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_flashcards_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_flashcards_user_status_updated ON flashcards (user_id, status, updated_at DESC);

CREATE TABLE flashcard_deck_items (
    deck_id uuid NOT NULL,
    flashcard_id uuid NOT NULL,
    sort_order integer,
    added_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_flashcard_deck_items PRIMARY KEY (deck_id, flashcard_id),
    CONSTRAINT fk_flashcard_deck_items_deck FOREIGN KEY (deck_id) REFERENCES flashcard_decks (id) ON DELETE CASCADE,
    CONSTRAINT fk_flashcard_deck_items_card FOREIGN KEY (flashcard_id) REFERENCES flashcards (id) ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_deck_items_deck_sort
    ON flashcard_deck_items (deck_id, sort_order, flashcard_id);

CREATE INDEX idx_flashcard_deck_items_flashcard
    ON flashcard_deck_items (flashcard_id);

-- Existing notes have no source. Source values are validated by the NoteSourceType enum.
ALTER TABLE notes ADD COLUMN source_type varchar(50);
ALTER TABLE notes ADD COLUMN source_reference_id uuid;
ALTER TABLE notes ADD CONSTRAINT chk_notes_source_pair
    CHECK ((source_type IS NULL) = (source_reference_id IS NULL));
CREATE INDEX idx_notes_user_source
    ON notes (user_id, source_type, source_reference_id, updated_at DESC)
    WHERE source_type IS NOT NULL;

-- One live flashcard per practised question and learner; a deleted card may be saved again.
-- PRACTICE_QUESTION cards point at the AI Learning practice question id through source_reference_id.
CREATE UNIQUE INDEX uq_flashcards_user_practice_question
    ON flashcards (user_id, source_reference_id)
    WHERE source_type = 'PRACTICE_QUESTION' AND status <> 'DELETED';

ALTER TABLE flashcards
    ADD CONSTRAINT fk_flashcards_vocabulary_sense
    FOREIGN KEY (vocabulary_sense_id) REFERENCES vocabulary_senses (id) ON DELETE RESTRICT;

ALTER TABLE video_learning_progress
    ADD CONSTRAINT fk_video_learning_progress_video
    FOREIGN KEY (video_id) REFERENCES learning_videos (id) ON DELETE RESTRICT;

ALTER TABLE saved_video_segments
    ADD CONSTRAINT fk_saved_video_segments_video
    FOREIGN KEY (video_id) REFERENCES learning_videos (id) ON DELETE RESTRICT;

ALTER TABLE saved_video_segments
    ADD CONSTRAINT fk_saved_video_segments_segment
    FOREIGN KEY (segment_id) REFERENCES video_segments (id) ON DELETE RESTRICT;
