ALTER TABLE game_match_players
    ADD COLUMN score_reached_at TIMESTAMPTZ,
    ADD COLUMN player_display_name VARCHAR(255),
    ADD COLUMN player_slot_number SMALLINT CHECK (player_slot_number BETWEEN 1 AND 4);

UPDATE game_match_players p SET
    score_reached_at = COALESCE(p.finished_at, p.joined_at),
    player_display_name = rm.display_name,
    player_slot_number = rm.slot_number
FROM game_room_members rm WHERE rm.id = p.room_member_id;
UPDATE game_match_players SET score_reached_at = joined_at WHERE score_reached_at IS NULL;
ALTER TABLE game_match_players ALTER COLUMN score_reached_at SET NOT NULL;
ALTER TABLE game_match_players ALTER COLUMN score_reached_at SET DEFAULT clock_timestamp();

CREATE UNIQUE INDEX uq_wordly_room_active_match ON game_matches(room_id)
    WHERE game_type = 'WORDLY' AND status = 'IN_PROGRESS' AND NOT is_delete;
