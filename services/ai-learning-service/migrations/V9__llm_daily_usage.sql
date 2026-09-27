-- How many LLM-backed actions each learner used per local day, so a learner cannot run up provider cost without limit.
CREATE TABLE llm_daily_usage (
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('tutor_turn', 'memory_summary')),
    used INTEGER NOT NULL CHECK (used >= 0),
    PRIMARY KEY (user_id, usage_date, kind)
);
