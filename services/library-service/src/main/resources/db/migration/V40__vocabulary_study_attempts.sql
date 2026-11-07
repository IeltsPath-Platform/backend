CREATE TABLE vocabulary_study_attempts (
    id UUID PRIMARY KEY,user_id UUID NOT NULL,topic_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id),revision INT NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK(mode IN ('FIRST_COMPLETION','REPLAY')),
    status VARCHAR(20) NOT NULL CHECK(status IN ('IN_PROGRESS','COMPLETED','EXPIRED')),
    started_at TIMESTAMPTZ NOT NULL,last_activity_at TIMESTAMPTZ NOT NULL,expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,start_request_id UUID NOT NULL,start_payload_hash CHAR(64) NOT NULL,
    graded_step_count INT NOT NULL,first_try_correct_count INT NOT NULL DEFAULT 0,completion JSONB,
    UNIQUE(user_id,start_request_id)
);
CREATE UNIQUE INDEX uq_vocab_study_attempt_active ON vocabulary_study_attempts(user_id,unit_id) WHERE status='IN_PROGRESS';
CREATE INDEX idx_vocab_study_attempt_user_topic ON vocabulary_study_attempts(user_id,topic_id);
-- Resume requests are logical starts too: every request id retains its original attempt and payload after completion.
CREATE TABLE vocabulary_study_start_requests (
    user_id UUID NOT NULL, request_id UUID NOT NULL,
    attempt_id UUID NOT NULL REFERENCES vocabulary_study_attempts(id) ON DELETE CASCADE,
    payload_hash CHAR(64) NOT NULL,
    PRIMARY KEY(user_id,request_id)
);
CREATE TABLE vocabulary_study_attempt_steps (
    attempt_id UUID NOT NULL REFERENCES vocabulary_study_attempts(id) ON DELETE CASCADE,step_id UUID NOT NULL,
    step_order INT NOT NULL,type VARCHAR(10) NOT NULL CHECK(type IN ('WORD','CHOICE','MATCH','FILL')),
    graded BOOLEAN NOT NULL,payload JSONB NOT NULL,answer_key JSONB NOT NULL,
    completed_at TIMESTAMPTZ,first_try_correct BOOLEAN,
    PRIMARY KEY(attempt_id,step_id),UNIQUE(attempt_id,step_order)
);
CREATE TABLE vocabulary_study_submissions (
    id UUID PRIMARY KEY,attempt_id UUID NOT NULL,step_id UUID NOT NULL,user_id UUID NOT NULL,request_id UUID NOT NULL,
    payload_hash CHAR(64) NOT NULL,answer JSONB NOT NULL,
    outcome VARCHAR(12) NOT NULL CHECK(outcome IN ('ACKNOWLEDGED','CORRECT','INCORRECT')),
    first_submission BOOLEAN NOT NULL,result JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(user_id,request_id),
    FOREIGN KEY(attempt_id,step_id) REFERENCES vocabulary_study_attempt_steps(attempt_id,step_id) ON DELETE CASCADE
);
CREATE INDEX idx_vocab_study_submission_step ON vocabulary_study_submissions(attempt_id,step_id,created_at);
CREATE TABLE vocabulary_study_unit_progress (
    user_id UUID NOT NULL,unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id),topic_id UUID NOT NULL,
    first_completed_at TIMESTAMPTZ NOT NULL,last_completed_at TIMESTAMPTZ NOT NULL,
    best_accuracy_percent INT NOT NULL,completed_revision INT NOT NULL,PRIMARY KEY(user_id,unit_id)
);
CREATE INDEX idx_vocab_study_progress_topic ON vocabulary_study_unit_progress(user_id,topic_id);
