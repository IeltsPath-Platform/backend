-- What a learner sees of an examiner when choosing who grades their work, and how many new jobs the examiner takes a day.
-- The name, photo and introduction come from learner_profiles; a row here is optional, so an examiner without one is
-- listed with the defaults below (accepting submissions, at most 10 new jobs a day, no band or certificates).
CREATE TABLE examiner_profiles (
    user_id UUID PRIMARY KEY,
    ielts_band NUMERIC(2, 1),
    certificates JSONB NOT NULL DEFAULT '[]'::jsonb,
    accepting_submissions BOOLEAN NOT NULL DEFAULT TRUE,
    daily_limit INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_examiner_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_examiner_profiles_band CHECK (ielts_band IS NULL
        OR (ielts_band BETWEEN 0 AND 9 AND ielts_band * 2 = trunc(ielts_band * 2))),
    CONSTRAINT chk_examiner_profiles_certificates CHECK (jsonb_typeof(certificates) = 'array'
        AND jsonb_array_length(certificates) <= 10),
    CONSTRAINT chk_examiner_profiles_daily_limit CHECK (daily_limit BETWEEN 1 AND 50)
);
