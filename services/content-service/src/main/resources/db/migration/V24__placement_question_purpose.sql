-- Questions of a placement test are kept apart from mock-test (EXAM) and learning questions.
ALTER TABLE questions DROP CONSTRAINT chk_questions_purpose;
ALTER TABLE questions ADD CONSTRAINT chk_questions_purpose
    CHECK (purpose IN ('LEARNING', 'EXAM', 'PLACEMENT'));

UPDATE questions q SET purpose = 'PLACEMENT'
WHERE EXISTS (SELECT 1 FROM question_versions qv
              JOIN section_questions sq ON sq.question_version_id = qv.id
              JOIN content_sections s ON s.id = sq.section_id
              JOIN content_package_versions v ON v.id = s.package_version_id
              JOIN content_packages p ON p.id = v.package_id
              WHERE qv.question_id = q.id AND p.package_type = 'PLACEMENT_TEST');
