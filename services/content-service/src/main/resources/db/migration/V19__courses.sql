CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL CONSTRAINT courses_code_key UNIQUE,
    name VARCHAR(255) NOT NULL,
    band_level NUMERIC(2,1) NOT NULL CONSTRAINT courses_band_level_key UNIQUE
        CHECK (band_level BETWEEN 0 AND 9 AND band_level * 2 = trunc(band_level * 2)),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE topics ADD COLUMN course_id UUID REFERENCES courses(id);
CREATE INDEX idx_topics_course_id ON topics(course_id);
