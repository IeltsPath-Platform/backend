-- Practice test library: the source of a test, the part number and cover image of a section, and groups of
-- questions inside a section. Everything is additive and nullable, so existing packages and sections are unchanged.

-- Source of a practice test (IELTS_SPACE_PRO, C10_C20, ...). The allowed codes are defined by the application, not by a
-- table; the database only checks the shape and that only a PRACTICE_TEST can have one.
ALTER TABLE content_packages ADD COLUMN source_code VARCHAR(40)
    CONSTRAINT chk_content_packages_source_code_format CHECK (source_code ~ '^[A-Z][A-Z0-9_]*$');
ALTER TABLE content_packages ADD CONSTRAINT chk_content_packages_source_practice_test
    CHECK (source_code IS NULL OR package_type = 'PRACTICE_TEST');
CREATE INDEX idx_content_packages_source ON content_packages(source_code) WHERE source_code IS NOT NULL;

ALTER TABLE content_packages DROP CONSTRAINT content_packages_package_type_check;
ALTER TABLE content_packages ADD CONSTRAINT content_packages_package_type_check
    CHECK (package_type IN ('MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'QUIZ', 'LESSON', 'TOPIC_TEST',
                            'COURSE_TEST', 'PRACTICE_TEST'));

-- A part is identified across package versions by (package, part_no); its section id changes with every version.
ALTER TABLE content_sections ADD COLUMN part_no INT CHECK (part_no > 0);
ALTER TABLE content_sections ADD COLUMN cover_asset_id UUID REFERENCES content_assets(id);
CREATE UNIQUE INDEX uq_content_sections_part_no
    ON content_sections(package_version_id, part_no) WHERE part_no IS NOT NULL;

-- A run of questions of the same kind inside a section ("Questions 1-5: complete the notes"). The allowed
-- format codes are defined by the application.
CREATE TABLE section_question_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES content_sections(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    format_code VARCHAR(40) NOT NULL CHECK (format_code ~ '^[A-Z][A-Z_]*$'),
    instructions TEXT,
    CONSTRAINT uq_section_question_groups_order UNIQUE (section_id, sort_order),
    CONSTRAINT uq_section_question_groups_id_section UNIQUE (id, section_id)
);

-- The composite key keeps a question in a group of its own section; group_id stays null for questions without a group.
ALTER TABLE section_questions ADD COLUMN group_id UUID;
ALTER TABLE section_questions ADD CONSTRAINT fk_section_questions_group
    FOREIGN KEY (group_id, section_id) REFERENCES section_question_groups(id, section_id);
CREATE INDEX idx_section_questions_group ON section_questions(group_id) WHERE group_id IS NOT NULL;
