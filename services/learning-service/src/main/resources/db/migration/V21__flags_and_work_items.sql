-- Learner anomaly flags raised by rules over the question event log and essays, reviewed by staff.
CREATE TABLE learner_flags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('TOO_FAST_HIGH_SCORE', 'BURST_SUBMISSIONS', 'DUPLICATE_ESSAY')),
    evidence JSONB NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CONFIRMED', 'DISMISSED')),
    reviewed_by UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ
);
CREATE INDEX idx_learner_flags_user ON learner_flags (user_id, kind, created_at DESC);
CREATE INDEX idx_learner_flags_open ON learner_flags (status) WHERE status = 'OPEN';

-- Work for staff. One row per (kind, subject), so a source can be synced again without duplicates.
CREATE TABLE work_items (
    id UUID PRIMARY KEY,
    queue VARCHAR(20) NOT NULL CHECK (queue IN ('EXAMINER', 'CONTENT_AUTHOR')),
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('MID_COURSE_REMEDIATION', 'POST_COURSE_REMEDIATION', 'LEARNER_FLAG',
        'CONTENT_GAP', 'QUESTION_TOO_EASY', 'QUESTION_TOO_HARD', 'EDGE_SUSPECT')),
    subject_user_id UUID,
    subject_ref UUID NOT NULL,
    detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE')),
    assignee_id UUID,
    action TEXT,
    result TEXT,
    note TEXT,
    note_visible_to_learner BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (kind, subject_ref)
);
CREATE INDEX idx_work_items_queue ON work_items (queue, status, created_at);
CREATE INDEX idx_work_items_learner_notes ON work_items (subject_user_id) WHERE note_visible_to_learner;

-- Normalised essay hash, to find the same essay sent by different learners.
ALTER TABLE lesson_writing_submissions ADD COLUMN essay_hash CHAR(64);
CREATE INDEX idx_lesson_writing_essay_hash ON lesson_writing_submissions (essay_hash) WHERE essay_hash IS NOT NULL;
