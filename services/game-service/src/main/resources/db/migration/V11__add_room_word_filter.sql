-- The level and topic a room draws its words from; null means any. Both games share these columns.
ALTER TABLE game_rooms
    ADD COLUMN cefr_level VARCHAR(2) CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    ADD COLUMN topic_id UUID;
