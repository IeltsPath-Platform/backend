ALTER TABLE assessment_attempts ADD COLUMN placement_pool VARCHAR(10);
ALTER TABLE assessment_attempts ADD CONSTRAINT ck_assessment_attempt_placement_pool
    CHECK (placement_pool IS NULL OR placement_pool IN ('BAND_0_3', 'BAND_3_5', 'BAND_5_9'));

ALTER TABLE assessment_results ADD COLUMN placement_pool VARCHAR(10);
ALTER TABLE assessment_results ADD COLUMN uncapped_band NUMERIC(3,1) CHECK (uncapped_band >= 0);
ALTER TABLE assessment_results ADD COLUMN partial BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assessment_results ADD CONSTRAINT ck_assessment_result_placement_pool
    CHECK (placement_pool IS NULL OR placement_pool IN ('BAND_0_3', 'BAND_3_5', 'BAND_5_9'));

CREATE INDEX idx_assessment_attempt_placement_limit
    ON assessment_attempts (user_id, attempt_type, created_at DESC);
