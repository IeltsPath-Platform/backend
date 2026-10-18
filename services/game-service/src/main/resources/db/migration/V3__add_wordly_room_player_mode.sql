ALTER TABLE game_rooms
    ADD COLUMN player_mode VARCHAR(20) NOT NULL DEFAULT 'MULTIPLAYER'
        CHECK (player_mode IN ('SOLO', 'MULTIPLAYER'));

ALTER TABLE game_rooms DROP CONSTRAINT ck_wordly_room;
ALTER TABLE game_rooms ADD CONSTRAINT ck_wordly_room CHECK (game_type <> 'WORDLY' OR (
    room_code ~ '^[A-Z0-9]{6}$' AND learning_domain = 'VOCABULARY'
    AND ((player_mode = 'SOLO' AND max_players = 1)
        OR (player_mode = 'MULTIPLAYER' AND max_players = 4))));
