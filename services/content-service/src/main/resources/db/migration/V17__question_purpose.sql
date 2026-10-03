ALTER TABLE questions ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'LEARNING'
    CONSTRAINT chk_questions_purpose CHECK (purpose IN ('LEARNING', 'EXAM'));

UPDATE questions q SET purpose = 'EXAM'
WHERE EXISTS (SELECT 1 FROM question_versions qv
              JOIN section_questions sq ON sq.question_version_id = qv.id
              JOIN content_sections s ON s.id = sq.section_id
              JOIN content_package_versions v ON v.id = s.package_version_id
              JOIN content_packages p ON p.id = v.package_id
              WHERE qv.question_id = q.id AND p.package_type IN ('MOCK_TEST', 'PLACEMENT_TEST'));

CREATE INDEX idx_questions_purpose ON questions (purpose);
