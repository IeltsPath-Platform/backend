-- Rocket: rooms and matches keep their own question count and time per question; lives reuse initial_lives.
-- Wordly columns stay untouched on Rocket rows, so a room can remember both games' settings.
ALTER TABLE game_rooms
    ADD COLUMN question_count SMALLINT CHECK (question_count BETWEEN 3 AND 20),
    ADD COLUMN seconds_per_question SMALLINT CHECK (seconds_per_question IN (10, 15, 20)),
    ADD CONSTRAINT ck_rocket_room CHECK (game_type <> 'ROCKET' OR (
        room_code ~ '^[A-Z0-9]{6}$' AND learning_domain = 'VOCABULARY'
        AND question_count IS NOT NULL AND seconds_per_question IS NOT NULL
        AND ((player_mode = 'SOLO' AND max_players = 1)
            OR (player_mode = 'MULTIPLAYER' AND max_players = 4 AND question_count >= 5))));

ALTER TABLE game_matches
    ADD COLUMN question_count SMALLINT CHECK (question_count BETWEEN 3 AND 20),
    ADD COLUMN seconds_per_question SMALLINT CHECK (seconds_per_question IN (10, 15, 20)),
    ADD CONSTRAINT ck_rocket_match CHECK (game_type <> 'ROCKET' OR (
        question_count IS NOT NULL AND seconds_per_question IS NOT NULL
        AND (player_mode = 'SOLO' OR question_count >= 5)));

-- One question of a match, shared by every player. All rounds are created when the match starts.
CREATE TABLE game_rocket_rounds (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES game_matches(id),
    sequence SMALLINT NOT NULL CHECK (sequence BETWEEN 1 AND 20),
    source_word_id UUID NOT NULL,
    meaning TEXT NOT NULL CHECK (length(btrim(meaning)) > 0),
    options JSONB NOT NULL CHECK (jsonb_typeof(options) = 'array' AND jsonb_array_length(options) = 3),
    correct_option_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'OPEN', 'CLOSED')),
    opens_at TIMESTAMPTZ,
    deadline_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    close_reason VARCHAR(20) CHECK (close_reason IN ('ALL_ANSWERED', 'DEADLINE', 'MATCH_ENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_delete BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_rocket_round_sequence UNIQUE (match_id, sequence),
    CONSTRAINT uq_rocket_round_match UNIQUE (id, match_id),
    -- A pending round may already know when it opens; deadline and closing come with opening and closing.
    CONSTRAINT ck_rocket_round_lifecycle CHECK (
        (status = 'PENDING' AND deadline_at IS NULL AND closed_at IS NULL AND close_reason IS NULL)
        OR (status = 'OPEN' AND opens_at IS NOT NULL AND deadline_at > opens_at
            AND closed_at IS NULL AND close_reason IS NULL)
        OR (status = 'CLOSED' AND opens_at IS NOT NULL AND deadline_at > opens_at
            AND closed_at IS NOT NULL AND close_reason IS NOT NULL)),
    CONSTRAINT ck_rocket_round_delete CHECK (is_delete = (deleted_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_rocket_round_open ON game_rocket_rounds(match_id) WHERE status = 'OPEN' AND NOT is_delete;
CREATE INDEX ix_rocket_round_deadline ON game_rocket_rounds(deadline_at) WHERE status = 'OPEN' AND NOT is_delete;
CREATE INDEX ix_rocket_round_scheduled ON game_rocket_rounds(opens_at)
    WHERE status = 'PENDING' AND opens_at IS NOT NULL AND NOT is_delete;

-- One row per player per round: the answer, or a miss written when the round closes without one.
CREATE TABLE game_rocket_answers (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL,
    match_id UUID NOT NULL,
    player_id UUID NOT NULL,
    option_id UUID,
    correct BOOLEAN NOT NULL,
    points SMALLINT NOT NULL CHECK (points BETWEEN 0 AND 200),
    received_at TIMESTAMPTZ,
    response_ms INTEGER NOT NULL CHECK (response_ms >= 0),
    request_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_delete BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_rocket_answer_round FOREIGN KEY (round_id, match_id) REFERENCES game_rocket_rounds(id, match_id),
    CONSTRAINT fk_rocket_answer_player FOREIGN KEY (player_id, match_id) REFERENCES game_match_players(id, match_id),
    CONSTRAINT uq_rocket_answer_player UNIQUE (round_id, player_id),
    CONSTRAINT ck_rocket_answer_shape CHECK (
        (option_id IS NULL AND request_id IS NULL AND received_at IS NULL AND NOT correct AND points = 0)
        OR (option_id IS NOT NULL AND request_id IS NOT NULL AND received_at IS NOT NULL)),
    CONSTRAINT ck_rocket_answer_points CHECK (correct = (points >= 100)),
    CONSTRAINT ck_rocket_answer_delete CHECK (is_delete = (deleted_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_rocket_answer_request ON game_rocket_answers(player_id, request_id) WHERE request_id IS NOT NULL;
