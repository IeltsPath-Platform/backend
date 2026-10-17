-- NULL means no per-word deadline; existing rooms and matches remain timed.
ALTER TABLE game_rooms ALTER COLUMN word_time_limit_seconds DROP NOT NULL;
ALTER TABLE game_rooms ADD CONSTRAINT ck_room_timer_mode CHECK (
    word_time_limit_seconds IS NOT NULL OR (game_type = 'WORDLY' AND player_mode = 'SOLO'));
ALTER TABLE game_matches ALTER COLUMN word_time_limit_seconds DROP NOT NULL;
ALTER TABLE game_matches ADD CONSTRAINT ck_match_timer_mode CHECK (
    word_time_limit_seconds IS NOT NULL OR (game_type = 'WORDLY' AND player_mode = 'SOLO'));
ALTER TABLE game_player_words ALTER COLUMN deadline_at DROP NOT NULL;
