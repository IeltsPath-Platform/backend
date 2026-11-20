CREATE TABLE code_settings (
    code_kind VARCHAR(32) PRIMARY KEY,
    applied BOOLEAN NOT NULL,
    pattern VARCHAR(255) NOT NULL,
    start_number BIGINT NOT NULL CHECK (start_number >= 0),
    editable BOOLEAN NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT code_settings_kind_check CHECK (code_kind IN (
        'COURSE', 'TOPIC', 'LESSON', 'PACKAGE', 'KNOWLEDGE_POINT', 'ERROR_TYPE', 'QUESTION'
    ))
);

CREATE TABLE code_counters (
    code_kind VARCHAR(32) NOT NULL,
    scope_key VARCHAR(255) NOT NULL DEFAULT '',
    next_number BIGINT NOT NULL CHECK (next_number >= 0),
    PRIMARY KEY (code_kind, scope_key),
    CONSTRAINT code_counters_kind_check CHECK (code_kind IN (
        'COURSE', 'TOPIC', 'LESSON', 'PACKAGE', 'KNOWLEDGE_POINT', 'ERROR_TYPE', 'QUESTION'
    ))
);

INSERT INTO code_settings (code_kind, applied, pattern, start_number, editable)
VALUES ('COURSE', TRUE, 'COURSE_{SEQ:3}', 1, TRUE);
