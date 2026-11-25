-- A part number only has to be unique among the sections of one skill: a mixed exam holds Listening Part 1 and
-- Reading Passage 1 side by side. Sections without a skill share one group. Existing rows already satisfy the
-- stricter rule, so nothing is rewritten.
DROP INDEX uq_content_sections_part_no;
CREATE UNIQUE INDEX uq_content_sections_part_no
    ON content_sections(package_version_id, COALESCE(skill, ''), part_no) WHERE part_no IS NOT NULL;
