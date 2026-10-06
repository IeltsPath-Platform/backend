CREATE TABLE learner_placements (
    user_id UUID PRIMARY KEY,
    band NUMERIC(2,1) NOT NULL CHECK (band BETWEEN 0 AND 9 AND band * 2 = trunc(band * 2)),
    attempt_id UUID NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
