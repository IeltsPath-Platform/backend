CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    match_player_id UUID UNIQUE,
    game_type VARCHAR(50) NOT NULL,
    learning_domain VARCHAR(30) NOT NULL CHECK (learning_domain IN ('VOCABULARY', 'GRAMMAR', 'MIXED')),
    mode VARCHAR(30) NOT NULL,
    topic_id UUID,
    source_snapshot JSONB NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    score INTEGER,
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE game_answers (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES game_sessions(id) ON DELETE CASCADE,
    item_sequence INTEGER NOT NULL CHECK (item_sequence > 0),
    vocabulary_sense_id UUID,
    question_version_id UUID,
    item_snapshot JSONB NOT NULL,
    response_payload JSONB NOT NULL,
    is_correct BOOLEAN NOT NULL,
    duration_milliseconds BIGINT NOT NULL CHECK (duration_milliseconds >= 0),
    CONSTRAINT uq_game_answers_session_sequence UNIQUE (session_id, item_sequence)
);
CREATE INDEX idx_game_answers_session ON game_answers(session_id, item_sequence);

CREATE TABLE game_rooms (
    id UUID PRIMARY KEY,
    room_code VARCHAR(20) NOT NULL UNIQUE,
    host_user_id UUID NOT NULL,
    game_type VARCHAR(50) NOT NULL,
    learning_domain VARCHAR(30) NOT NULL CHECK (learning_domain IN ('VOCABULARY', 'GRAMMAR', 'MIXED')),
    mode VARCHAR(30) NOT NULL,
    max_players INTEGER NOT NULL CHECK (max_players > 0),
    config_snapshot JSONB NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('WAITING', 'IN_MATCH', 'CLOSED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ
);
CREATE INDEX idx_game_rooms_status_created ON game_rooms(status, created_at DESC);

CREATE TABLE game_room_members (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES game_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    member_role VARCHAR(20) NOT NULL CHECK (member_role IN ('HOST', 'PLAYER')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('JOINED', 'READY', 'LEFT', 'DISCONNECTED')),
    joined_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    CONSTRAINT uq_game_room_members_room_user UNIQUE (room_id, user_id)
);
CREATE INDEX idx_game_room_members_user_status ON game_room_members(user_id, status);

CREATE TABLE game_matches (
    id UUID PRIMARY KEY,
    room_id UUID REFERENCES game_rooms(id),
    game_type VARCHAR(50) NOT NULL,
    learning_domain VARCHAR(30) NOT NULL CHECK (learning_domain IN ('VOCABULARY', 'GRAMMAR', 'MIXED')),
    config_snapshot JSONB NOT NULL,
    content_snapshot JSONB NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_game_matches_room_created ON game_matches(room_id, created_at DESC);

CREATE TABLE game_match_players (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
    room_member_id UUID REFERENCES game_room_members(id),
    user_id UUID NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    rank INTEGER CHECK (rank > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'FINISHED', 'DISCONNECTED', 'FORFEITED')),
    joined_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    CONSTRAINT uq_game_match_players_match_user UNIQUE (match_id, user_id)
);
CREATE INDEX idx_game_match_players_user_joined ON game_match_players(user_id, joined_at DESC);
ALTER TABLE game_sessions
    ADD CONSTRAINT fk_game_sessions_match_player FOREIGN KEY (match_player_id)
    REFERENCES game_match_players(id);

CREATE TABLE game_events (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
    match_player_id UUID REFERENCES game_match_players(id),
    sequence_no BIGINT NOT NULL CHECK (sequence_no > 0),
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_game_events_match_sequence UNIQUE (match_id, sequence_no)
);
CREATE INDEX idx_game_events_match_sequence ON game_events(match_id, sequence_no);

CREATE TABLE quiz_events (
    id UUID PRIMARY KEY,
    package_version_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    opens_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_quiz_events_window CHECK (closes_at IS NULL OR closes_at > opens_at)
);
CREATE INDEX idx_quiz_events_status_opens ON quiz_events(status, opens_at);

CREATE TABLE quiz_participations (
    id UUID PRIMARY KEY,
    quiz_event_id UUID NOT NULL REFERENCES quiz_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    attempt_id UUID NOT NULL UNIQUE,
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    score INTEGER,
    duration_milliseconds BIGINT CHECK (duration_milliseconds >= 0),
    verification_status VARCHAR(30) NOT NULL CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_quiz_participations_event_user_attempt UNIQUE (quiz_event_id, user_id, attempt_number)
);
CREATE INDEX idx_quiz_participations_event_score ON quiz_participations(quiz_event_id, score DESC, duration_milliseconds ASC);
CREATE INDEX idx_quiz_participations_user_created ON quiz_participations(user_id, created_at DESC);

CREATE TABLE leaderboard_periods (
    id UUID PRIMARY KEY,
    topic_id UUID,
    activity_type VARCHAR(50) NOT NULL,
    period_type VARCHAR(30) NOT NULL CHECK (period_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME', 'CUSTOM')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    timezone VARCHAR(100) NOT NULL,
    rules_version VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_leaderboard_period_window CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX idx_leaderboard_periods_activity_window ON leaderboard_periods(activity_type, starts_at, ends_at);

CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL REFERENCES leaderboard_periods(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    score BIGINT NOT NULL,
    tie_break_duration_milliseconds BIGINT CHECK (tie_break_duration_milliseconds >= 0),
    rank BIGINT CHECK (rank > 0),
    calculated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_leaderboard_entries_period_user UNIQUE (period_id, user_id)
);
CREATE INDEX idx_leaderboard_entries_rank ON leaderboard_entries(period_id, rank, score DESC, tie_break_duration_milliseconds ASC, user_id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    last_error TEXT
);
CREATE INDEX idx_outbox_events_pending ON outbox_events(published_at, created_at);
