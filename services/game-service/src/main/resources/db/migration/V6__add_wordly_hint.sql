ALTER TABLE game_player_words
 ADD COLUMN hint_request_id UUID UNIQUE,
 ADD COLUMN hint_source_guess_id UUID REFERENCES game_guesses(id),
 ADD COLUMN hint_column INTEGER CHECK (hint_column >= 0 AND hint_column < 100),
 ADD COLUMN hint_letter VARCHAR(1) CHECK (hint_letter ~ '^[A-Z]$'),
 ADD COLUMN hint_used_at TIMESTAMPTZ,
 ADD CONSTRAINT ck_word_hint CHECK (
  (hint_request_id IS NULL AND hint_source_guess_id IS NULL AND hint_column IS NULL AND hint_letter IS NULL AND hint_used_at IS NULL)
  OR (hint_request_id IS NOT NULL AND hint_source_guess_id IS NOT NULL AND hint_column IS NOT NULL AND hint_letter IS NOT NULL AND hint_used_at IS NOT NULL));
