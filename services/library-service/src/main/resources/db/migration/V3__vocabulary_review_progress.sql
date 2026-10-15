CREATE TABLE learner_vocabulary_progress (
 user_id UUID NOT NULL, vocabulary_sense_id UUID NOT NULL REFERENCES vocabulary_senses(id),
 state VARCHAR(16) NOT NULL CHECK (state IN ('NEW','L1','L2','L3','RETAINED')),
 state_version BIGINT NOT NULL CHECK (state_version >= 0), active BOOLEAN NOT NULL,
 next_review_at TIMESTAMPTZ NOT NULL, last_reviewed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(user_id,vocabulary_sense_id)
);
CREATE INDEX idx_vocabulary_progress_due ON learner_vocabulary_progress(user_id,active,next_review_at,vocabulary_sense_id);
CREATE TABLE vocabulary_review_sessions (
 id UUID PRIMARY KEY, user_id UUID NOT NULL, request_id UUID NOT NULL,
 queue_limit INTEGER NOT NULL CHECK (queue_limit BETWEEN 1 AND 50),
 created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL,
 UNIQUE(user_id,request_id)
);
CREATE TABLE vocabulary_review_items (
 session_id UUID NOT NULL REFERENCES vocabulary_review_sessions(id), vocabulary_sense_id UUID NOT NULL REFERENCES vocabulary_senses(id),
 state_version BIGINT NOT NULL, sort_order INTEGER NOT NULL, lemma TEXT NOT NULL, meaning TEXT NOT NULL,
 example_sentence TEXT NOT NULL, answered BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(session_id,vocabulary_sense_id)
);
CREATE TABLE vocabulary_review_events (
 user_id UUID NOT NULL, request_id UUID NOT NULL, session_id UUID NOT NULL,
 vocabulary_sense_id UUID NOT NULL, expected_version BIGINT NOT NULL,
 outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('REMEMBERED','FORGOT')),
 recall_source VARCHAR(16) NOT NULL CHECK(recall_source='SELF_RATED'), schedule_version INTEGER NOT NULL,
 resulting_state VARCHAR(16) NOT NULL, resulting_version BIGINT NOT NULL,
 next_review_at TIMESTAMPTZ NOT NULL, reviewed_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(user_id,request_id), UNIQUE(user_id,vocabulary_sense_id,expected_version),
 FOREIGN KEY(session_id,vocabulary_sense_id) REFERENCES vocabulary_review_items(session_id,vocabulary_sense_id)
);
