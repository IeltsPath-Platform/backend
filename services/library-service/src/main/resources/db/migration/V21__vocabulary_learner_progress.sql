CREATE TABLE flashcard_review_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    flashcard_id UUID NOT NULL,
    vocabulary_sense_id UUID,
    session_id UUID,
    correct BOOLEAN NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_flashcard_review_events_user_reviewed ON flashcard_review_events(user_id, reviewed_at DESC);
CREATE INDEX idx_flashcard_review_events_user_session ON flashcard_review_events(user_id, session_id, reviewed_at DESC);
CREATE INDEX idx_flashcard_review_events_user_sense_reviewed ON flashcard_review_events(user_id, vocabulary_sense_id, reviewed_at DESC);

CREATE TABLE vocabulary_weekly_goals (
    user_id UUID PRIMARY KEY,
    target_words INT NOT NULL CHECK (target_words BETWEEN 10 AND 1000),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
