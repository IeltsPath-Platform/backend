CREATE TABLE game_match_word_pool (
 match_id UUID NOT NULL REFERENCES game_matches(id), position INTEGER NOT NULL CHECK(position>0),
 source_word_id UUID NOT NULL, answer VARCHAR(100) NOT NULL CHECK(answer ~ '^[A-Z]+$'),meaning TEXT NOT NULL,
 PRIMARY KEY(match_id,position),UNIQUE(match_id,answer)
);
CREATE TABLE game_guess_receipts (
 request_id UUID PRIMARY KEY,match_id UUID NOT NULL REFERENCES game_matches(id),
 player_word_id UUID NOT NULL REFERENCES game_player_words(id),user_id UUID NOT NULL,
 guess VARCHAR(100) NOT NULL,received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 http_status INTEGER,response JSONB,
 CHECK((http_status IS NULL AND response IS NULL) OR (http_status IS NOT NULL AND response IS NOT NULL))
);
CREATE INDEX idx_guess_receipts_pending ON game_guess_receipts(match_id,received_at,request_id) WHERE http_status IS NULL;
ALTER TABLE game_match_players ALTER COLUMN score TYPE BIGINT;
