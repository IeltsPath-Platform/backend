INSERT INTO code_settings (code_kind, applied, pattern, start_number, editable)
VALUES
    ('LESSON', TRUE, '{TOPIC}_L{SEQ:2}', 1, TRUE),
    ('PACKAGE', TRUE, '{COURSE}_{TYPE}{SEQ:2}', 1, TRUE);
