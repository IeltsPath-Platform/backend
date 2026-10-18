-- Time the learner spent on an item, in milliseconds. The client reports a running total; the server keeps the largest value
-- it has seen and caps it at two hours, so a stalled tab cannot inflate the figure.
ALTER TABLE attempt_items ADD COLUMN elapsed_ms BIGINT CHECK (elapsed_ms IS NULL OR elapsed_ms >= 0);
