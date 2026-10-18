-- Hearts: a wrong objective answer in a lesson, a lesson Practice set or free practice costs one heart. Hearts regain one
-- at a time every five hours while below the maximum; the clock stops once they are full.
CREATE TABLE learner_hearts (
    user_id UUID PRIMARY KEY,
    hearts SMALLINT NOT NULL CHECK (hearts >= 0),
    max_hearts SMALLINT NOT NULL CHECK (max_hearts >= 1),
    last_regen_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (hearts <= max_hearts)
);
