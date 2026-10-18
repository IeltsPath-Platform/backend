ALTER TABLE knowledge_points
    ADD COLUMN cefr_level VARCHAR(2)
        CONSTRAINT knowledge_points_cefr_level_check
        CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    ADD COLUMN can_do TEXT,
    ADD COLUMN scope_in TEXT,
    ADD COLUMN scope_out TEXT;
