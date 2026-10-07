-- A practice set can mix skills. An attempt keeps the skills of its set and the skills it passed (70% or more of
-- that skill's questions); the single skill column stays for older readers and is NULL for a mixed set.
ALTER TABLE practice_attempts ADD COLUMN skills VARCHAR(20)[] NOT NULL DEFAULT '{}';
ALTER TABLE practice_attempts ADD COLUMN passed_skills VARCHAR(20)[] NOT NULL DEFAULT '{}';
UPDATE practice_attempts SET skills = ARRAY[skill];
UPDATE practice_attempts SET passed_skills = ARRAY[skill] WHERE passed = TRUE;
ALTER TABLE practice_attempts ALTER COLUMN skill DROP NOT NULL;
