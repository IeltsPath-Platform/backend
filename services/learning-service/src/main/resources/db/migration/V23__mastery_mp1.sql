-- MP-1.0 mastery: a point without enough evidence has the INSUFFICIENT tier, the profile keeps the decayed weight it
-- was judged on, and evidence remembers which question version it came from so the same question inside a week
-- counts once. Evidence written before this has no question and is never treated as a repeat.
ALTER TABLE kp_mastery_profiles DROP CONSTRAINT kp_mastery_profiles_tier_check;
ALTER TABLE kp_mastery_profiles ADD CONSTRAINT kp_mastery_profiles_tier_check
    CHECK (tier IN ('PRIORITY', 'CONSOLIDATE', 'MAINTAIN', 'INSUFFICIENT'));
ALTER TABLE kp_mastery_profiles ADD COLUMN confidence NUMERIC(6,3) CHECK (confidence IS NULL OR confidence >= 0);

ALTER TABLE kp_evidence ADD COLUMN question_version_id UUID;

-- The decay job looks for profiles whose newest evidence is older than a day.
CREATE INDEX idx_kp_mastery_last_evidence ON kp_mastery_profiles (last_evidence_at);
