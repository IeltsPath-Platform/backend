CREATE TABLE learning_activities (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    activity_type varchar(50) NOT NULL,
    source_type varchar(50) NOT NULL,
    source_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    duration_seconds integer,
    verified_at timestamptz,
    CONSTRAINT chk_learning_activities_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX idx_learning_activities_user_occurred
    ON learning_activities (user_id, occurred_at DESC);

CREATE TABLE streaks (
    user_id uuid PRIMARY KEY,
    current_days integer NOT NULL DEFAULT 0,
    longest_days integer NOT NULL DEFAULT 0,
    last_qualified_date date,
    timezone varchar(100) NOT NULL
);

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

CREATE TABLE outbox_events (
    id uuid PRIMARY KEY,
    aggregate_type varchar(100) NOT NULL,
    aggregate_id varchar(255) NOT NULL,
    event_type varchar(150) NOT NULL,
    payload jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    retry_count integer NOT NULL DEFAULT 0,
    last_error text
);

CREATE INDEX idx_outbox_events_published_created
    ON outbox_events (published_at, created_at);
