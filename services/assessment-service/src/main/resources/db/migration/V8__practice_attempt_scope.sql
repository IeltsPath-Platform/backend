ALTER TABLE assessment_attempts ADD COLUMN package_id UUID;
ALTER TABLE assessment_attempts ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'FULL';
ALTER TABLE assessment_attempts ADD COLUMN skill VARCHAR(20);

ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_scope
    CHECK (scope IN ('FULL', 'PART'));
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_skill
    CHECK (skill IS NULL OR skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING'));
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_part_practice
    CHECK (scope = 'FULL' OR attempt_type = 'OFFICIAL_PRACTICE');
ALTER TABLE assessment_attempts ADD CONSTRAINT chk_assessment_attempts_skill_practice
    CHECK (skill IS NULL OR attempt_type = 'OFFICIAL_PRACTICE');
-- Legacy official-practice attempts may have no package/skill identity. Keep those rows updateable,
-- while requiring identity on every new practice attempt and every identity-changing update.
CREATE FUNCTION enforce_official_practice_attempt_identity() RETURNS trigger AS $$
BEGIN
    IF NEW.attempt_type = 'OFFICIAL_PRACTICE' AND (NEW.package_id IS NULL OR NEW.skill IS NULL) THEN
        IF TG_OP = 'UPDATE'
            AND OLD.attempt_type = 'OFFICIAL_PRACTICE'
            AND OLD.package_id IS NOT DISTINCT FROM NEW.package_id
            AND OLD.skill IS NOT DISTINCT FROM NEW.skill THEN
            RETURN NEW;
        END IF;
        RAISE EXCEPTION 'Official practice attempts require package_id and skill'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_official_practice_attempt_identity
    BEFORE INSERT OR UPDATE OF attempt_type, package_id, skill ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION enforce_official_practice_attempt_identity();

ALTER TABLE attempt_sections ADD COLUMN part_no INT CHECK (part_no > 0);

CREATE INDEX idx_assessment_attempts_user_package
    ON assessment_attempts(user_id, package_id) WHERE package_id IS NOT NULL;
