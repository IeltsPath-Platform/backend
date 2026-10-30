ALTER TABLE courses
    ADD COLUMN combo_unlock_points INTEGER;

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_combo_unlock_points_positive
        CHECK (combo_unlock_points IS NULL OR combo_unlock_points > 0);

CREATE TABLE course_skill_prices (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    skill VARCHAR(16) NOT NULL CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING')),
    unlock_points INTEGER NOT NULL CHECK (unlock_points > 0),
    PRIMARY KEY (course_id, skill)
);
