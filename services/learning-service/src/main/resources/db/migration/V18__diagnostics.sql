-- Prerequisite diagnosis: when a blocking review of knowledge point B keeps failing, short probes of the points B
-- builds on find the real gap. A session belongs to one review; its probes form the result matrix.
CREATE TABLE diagnostic_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    root_review_id UUID NOT NULL REFERENCES review_items (id),
    root_kp_id UUID NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('PROBING', 'CONCLUDED')),
    -- The gap points found; the root point itself when no probe failed.
    conclusion_kp_ids UUID[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    concluded_at TIMESTAMPTZ,
    UNIQUE (root_review_id)
);
CREATE INDEX idx_diagnostic_sessions_user ON diagnostic_sessions (user_id, status);

CREATE TABLE diagnostic_probes (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES diagnostic_sessions (id),
    kp_id UUID NOT NULL,
    parent_kp_id UUID NOT NULL,
    depth SMALLINT NOT NULL CHECK (depth BETWEEN 1 AND 3),
    package_id UUID,
    package_version_id UUID,
    -- NO_SET: no unseen set was left, so the point stays unknown instead of counting as a gap.
    status VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'ANSWERED', 'NO_SET')),
    correct INTEGER,
    total INTEGER,
    passed BOOLEAN,
    request_id UUID UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMPTZ,
    UNIQUE (session_id, kp_id)
);

-- Knowledge points whose reviews ran out of unseen sets, for the content team.
CREATE TABLE content_gap_signals (
    kp_id UUID PRIMARY KEY,
    occurrences INTEGER NOT NULL,
    first_detected_at TIMESTAMPTZ NOT NULL,
    last_detected_at TIMESTAMPTZ NOT NULL
);

-- The review of B waits for the reviews of the gap points before it gives a new set.
ALTER TABLE review_items ADD COLUMN waiting_on_review_ids UUID[] NOT NULL DEFAULT '{}';

ALTER TABLE kp_evidence DROP CONSTRAINT kp_evidence_source_check;
ALTER TABLE kp_evidence ADD CONSTRAINT kp_evidence_source_check CHECK (source IN
    ('lesson_exercise', 'review_set', 'assessment', 'lesson_writing', 'practice_set', 'diagnostic_probe'));
