-- One row per time a learner types a video segment back in dictation practice.
-- accuracy_percent is the share of the segment's words the learner typed correctly (0-100); is_correct is the
-- pass decision the application took from it, so a later threshold change does not rewrite old results.
-- Progress on a video (segments done, segments correct) is computed from these rows, not stored separately.
CREATE TABLE video_dictation_attempts (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL,
    segment_id uuid NOT NULL,
    attempt_no integer NOT NULL,
    typed_text text NOT NULL,
    is_correct boolean NOT NULL,
    accuracy_percent numeric(5, 2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_video_dictation_attempts_user_segment_attempt UNIQUE (user_id, segment_id, attempt_no),
    CONSTRAINT chk_video_dictation_attempts_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT chk_video_dictation_attempts_accuracy CHECK (accuracy_percent >= 0 AND accuracy_percent <= 100),
    CONSTRAINT fk_video_dictation_attempts_video
        FOREIGN KEY (video_id) REFERENCES learning_videos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_video_dictation_attempts_segment
        FOREIGN KEY (segment_id) REFERENCES video_segments (id) ON DELETE RESTRICT
);

CREATE INDEX idx_video_dictation_attempts_user_video
    ON video_dictation_attempts (user_id, video_id);
