-- Preserve legacy game types and their data while introducing Wordly.
ALTER TABLE game_rooms
    ADD COLUMN initial_lives SMALLINT NOT NULL DEFAULT 4 CHECK (initial_lives BETWEEN 3 AND 6),
    ADD COLUMN word_time_limit_seconds SMALLINT NOT NULL DEFAULT 180 CHECK (word_time_limit_seconds = 180),
    ADD COLUMN play_mode VARCHAR(20) NOT NULL DEFAULT 'FINITE' CHECK (play_mode IN ('FINITE', 'ENDLESS')),
    ADD COLUMN word_limit INTEGER DEFAULT 10,
    ADD CONSTRAINT ck_room_word_limit CHECK (
        (play_mode = 'FINITE' AND word_limit IS NOT NULL AND word_limit >= 5)
        OR (play_mode = 'ENDLESS' AND word_limit IS NULL)),
    ADD CONSTRAINT ck_wordly_room CHECK (game_type <> 'WORDLY'
        OR (room_code ~ '^[A-Z0-9]{6}$' AND max_players = 4));

-- Existing member status JOINED/READY remains the source of readiness.
ALTER TABLE game_room_members ADD COLUMN slot_number SMALLINT CHECK (slot_number BETWEEN 1 AND 4);

ALTER TABLE game_matches
    ADD COLUMN player_mode VARCHAR(20),
    ADD COLUMN play_mode VARCHAR(20) NOT NULL DEFAULT 'FINITE' CHECK (play_mode IN ('FINITE', 'ENDLESS')),
    ADD COLUMN initial_lives SMALLINT NOT NULL DEFAULT 4 CHECK (initial_lives BETWEEN 3 AND 6),
    ADD COLUMN word_time_limit_seconds SMALLINT NOT NULL DEFAULT 180 CHECK (word_time_limit_seconds = 180),
    ADD COLUMN word_limit INTEGER DEFAULT 10,
    ADD COLUMN end_reason VARCHAR(40),
    ADD CONSTRAINT ck_match_word_limit CHECK (
        (play_mode = 'FINITE' AND word_limit IS NOT NULL AND word_limit >= 5)
        OR (play_mode = 'ENDLESS' AND word_limit IS NULL));
UPDATE game_matches SET player_mode = CASE WHEN room_id IS NULL THEN 'SOLO' ELSE 'MULTIPLAYER' END;
ALTER TABLE game_matches
    ALTER COLUMN player_mode SET NOT NULL,
    ALTER COLUMN player_mode SET DEFAULT 'MULTIPLAYER',
    ADD CONSTRAINT ck_match_player_mode CHECK (player_mode IN ('SOLO', 'MULTIPLAYER'));

-- Keep existing score/rank column names and Java mappings; score is the total score.
ALTER TABLE game_match_players
    ADD COLUMN lives_remaining SMALLINT NOT NULL DEFAULT 4 CHECK (lives_remaining BETWEEN 0 AND 6),
    ADD COLUMN time_score BIGINT NOT NULL DEFAULT 0 CHECK (time_score >= 0),
    ADD COLUMN bonus_score BIGINT NOT NULL DEFAULT 0 CHECK (bonus_score >= 0),
    ADD COLUMN current_streak INTEGER NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    ADD COLUMN best_streak INTEGER NOT NULL DEFAULT 0 CHECK (best_streak >= current_streak),
    ADD COLUMN words_processed INTEGER NOT NULL DEFAULT 0 CHECK (words_processed >= 0),
    ADD COLUMN words_solved INTEGER NOT NULL DEFAULT 0 CHECK (words_solved BETWEEN 0 AND words_processed),
    ADD COLUMN total_solve_time_ms BIGINT NOT NULL DEFAULT 0 CHECK (total_solve_time_ms >= 0),
    ADD COLUMN disconnected_at TIMESTAMPTZ,
    ADD COLUMN reconnect_deadline_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_player_reconnect CHECK (
        (disconnected_at IS NULL AND reconnect_deadline_at IS NULL)
        OR (disconnected_at IS NOT NULL AND reconnect_deadline_at IS NOT NULL
            AND reconnect_deadline_at = disconnected_at + INTERVAL '30 seconds'));
ALTER TABLE game_match_players DROP CONSTRAINT game_match_players_status_check;
ALTER TABLE game_match_players ADD CONSTRAINT game_match_players_status_check
    CHECK (status IN ('ACTIVE', 'FINISHED', 'DISCONNECTED', 'FORFEITED', 'ELIMINATED'));

CREATE TABLE game_match_words (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES game_matches(id),
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    source_word_id UUID NOT NULL,
    answer VARCHAR(100) NOT NULL CHECK (answer ~ '^[A-Z]+$'),
    meaning TEXT NOT NULL CHECK (length(btrim(meaning)) > 0),
    CONSTRAINT uq_match_word_sequence UNIQUE (match_id, sequence_number),
    CONSTRAINT uq_match_word_id_match UNIQUE (id, match_id)
);

ALTER TABLE game_match_players ADD CONSTRAINT uq_match_player_id_match UNIQUE (id, match_id);
CREATE TABLE game_player_words (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES game_matches(id),
    match_player_id UUID NOT NULL,
    match_word_id UUID NOT NULL,
    attempts_used SMALLINT NOT NULL DEFAULT 0 CHECK (attempts_used >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLVED', 'FAILED', 'TIMED_OUT', 'CANCELLED')),
    started_at TIMESTAMPTZ NOT NULL,
    deadline_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    solve_time_ms BIGINT CHECK (solve_time_ms >= 0),
    time_points INTEGER NOT NULL DEFAULT 0 CHECK (time_points BETWEEN 0 AND 180),
    bonus_points INTEGER NOT NULL DEFAULT 0 CHECK (bonus_points IN (0, 50)),
    CONSTRAINT fk_player_word_player FOREIGN KEY (match_player_id, match_id)
        REFERENCES game_match_players(id, match_id),
    CONSTRAINT fk_player_word_word FOREIGN KEY (match_word_id, match_id)
        REFERENCES game_match_words(id, match_id),
    CONSTRAINT uq_player_word UNIQUE (match_player_id, match_word_id),
    CONSTRAINT ck_player_word_deadline CHECK (deadline_at = started_at + INTERVAL '180 seconds'),
    CONSTRAINT ck_player_word_finish CHECK (
        (status = 'ACTIVE' AND finished_at IS NULL)
        OR (status <> 'ACTIVE' AND finished_at IS NOT NULL AND finished_at >= started_at)),
    CONSTRAINT ck_player_word_solve_time CHECK (
        (status = 'SOLVED' AND solve_time_ms IS NOT NULL)
        OR (status <> 'SOLVED' AND solve_time_ms IS NULL))
);

CREATE TABLE game_guesses (
    id UUID PRIMARY KEY,
    player_word_id UUID NOT NULL REFERENCES game_player_words(id),
    request_id UUID NOT NULL UNIQUE,
    attempt_number SMALLINT NOT NULL CHECK (attempt_number > 0),
    guess VARCHAR(100) NOT NULL CHECK (guess ~ '^[A-Z]+$'),
    letter_results JSONB NOT NULL,
    is_correct BOOLEAN NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_guess_attempt UNIQUE (player_word_id, attempt_number),
    CONSTRAINT ck_guess_letters CHECK (
        CASE WHEN jsonb_typeof(letter_results) = 'array' THEN
            jsonb_array_length(letter_results) = length(guess)
            AND letter_results <@ '["CORRECT", "PRESENT", "ABSENT"]'::jsonb
        ELSE FALSE END)
);

-- Soft deletion only; no purge job or cascading soft deletion.
DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['game_rooms', 'game_room_members', 'game_matches',
        'game_match_players', 'game_match_words', 'game_player_words', 'game_guesses']
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN is_delete BOOLEAN NOT NULL DEFAULT FALSE,
            ADD COLUMN deleted_at TIMESTAMPTZ,
            ADD CONSTRAINT %I CHECK ((is_delete AND deleted_at IS NOT NULL)
                OR (NOT is_delete AND deleted_at IS NULL))', table_name, 'ck_' || table_name || '_deletion');
    END LOOP;
END $$;

CREATE UNIQUE INDEX uq_room_active_slot ON game_room_members(room_id, slot_number)
    WHERE NOT is_delete AND status IN ('JOINED', 'READY', 'DISCONNECTED');
CREATE UNIQUE INDEX uq_player_active_word ON game_player_words(match_player_id)
    WHERE NOT is_delete AND status = 'ACTIVE';
CREATE INDEX idx_wordly_deadlines ON game_player_words(deadline_at)
    WHERE NOT is_delete AND status = 'ACTIVE';
CREATE INDEX idx_match_player_ranking ON game_match_players(match_id, score DESC, total_solve_time_ms ASC)
    WHERE NOT is_delete;
CREATE INDEX idx_player_reconnect ON game_match_players(reconnect_deadline_at)
    WHERE NOT is_delete AND disconnected_at IS NOT NULL AND status IN ('ACTIVE', 'DISCONNECTED');
