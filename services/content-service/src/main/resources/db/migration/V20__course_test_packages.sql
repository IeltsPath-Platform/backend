ALTER TABLE content_packages DROP CONSTRAINT content_packages_package_type_check;
ALTER TABLE content_packages ADD CONSTRAINT content_packages_package_type_check
    CHECK (package_type IN ('MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'QUIZ', 'LESSON', 'TOPIC_TEST', 'COURSE_TEST'));

ALTER TABLE content_packages ADD COLUMN course_id UUID REFERENCES courses(id);
ALTER TABLE content_packages ADD CONSTRAINT chk_content_packages_course_test_course
    CHECK (package_type <> 'COURSE_TEST' OR course_id IS NOT NULL);
CREATE INDEX idx_content_packages_course_id ON content_packages(course_id);
