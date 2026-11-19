ALTER TABLE kp_mastery_profiles
    ADD COLUMN decayed_from_tier VARCHAR(20);

ALTER TABLE kp_mastery_profiles
    ADD CONSTRAINT kp_mastery_profiles_decayed_from_tier_check
        CHECK (decayed_from_tier IS NULL OR decayed_from_tier IN ('CONSOLIDATE', 'MAINTAIN'));

CREATE INDEX kp_mastery_profiles_decayed_due_idx
    ON kp_mastery_profiles (user_id, last_evidence_at, knowledge_point_id)
    WHERE tier = 'INSUFFICIENT' AND decayed_from_tier IS NOT NULL;
