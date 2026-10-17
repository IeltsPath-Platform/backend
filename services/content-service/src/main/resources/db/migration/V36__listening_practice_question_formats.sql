-- The Listening practice library is filtered by the question types learners know from the real test: Gap Filling,
-- Map/Diagram Label, Multiple Choice (one answer), Matching Information, Multiple Choice (many answers) and Matching.
-- The demo Listening tests used finer Reading-style names; fold them into the matching type.
UPDATE section_question_groups g
SET format_code = CASE g.format_code
        WHEN 'FORM_COMPLETION' THEN 'GAP_FILLING'
        WHEN 'NOTE_COMPLETION' THEN 'GAP_FILLING'
        WHEN 'SUMMARY_COMPLETION' THEN 'GAP_FILLING'
        WHEN 'MATCHING_FEATURES' THEN 'MATCHING'
    END
FROM content_sections s
JOIN content_package_versions v ON v.id = s.package_version_id
JOIN content_packages p ON p.id = v.package_id
WHERE g.section_id = s.id
  AND s.skill = 'LISTENING'
  AND p.package_type = 'PRACTICE_TEST'
  AND g.format_code IN ('FORM_COMPLETION', 'NOTE_COMPLETION', 'SUMMARY_COMPLETION', 'MATCHING_FEATURES');
