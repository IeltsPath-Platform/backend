-- A lobby member whose last socket closed keeps the seat until this time, then leaves the room.
ALTER TABLE game_room_members ADD COLUMN reconnect_deadline_at TIMESTAMPTZ;

CREATE INDEX idx_game_room_members_reconnect_deadline ON game_room_members(reconnect_deadline_at)
    WHERE status = 'DISCONNECTED' AND NOT is_delete;
