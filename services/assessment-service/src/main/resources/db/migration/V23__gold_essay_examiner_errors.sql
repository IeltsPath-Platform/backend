-- Examiner-marked errors on a gold essay: the excerpt is an exact substring of the essay; kind says whether
-- the error is about language (LANGUAGE) or about the chart's data (DATA, Task 1 only).
ALTER TABLE ai_gold_essays
    ADD COLUMN examiner_errors JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Run-level measurements added to the agreement metrics. Runs made before this migration keep NULL.
ALTER TABLE ai_eval_runs
    ADD COLUMN error_recall NUMERIC(6,5),
    ADD COLUMN data_error_recall NUMERIC(6,5),
    ADD COLUMN corrections_per_essay NUMERIC(6,2),
    ADD COLUMN p95_latency_ms INTEGER,
    ADD COLUMN invalid_grade_rate NUMERIC(6,5),
    ADD COLUMN reasoning_effort VARCHAR(20);

-- Per-essay detail for reviewing a run: what the model said, how long it took, and why an essay failed.
ALTER TABLE ai_eval_items
    ADD COLUMN llm_corrections JSONB,
    ADD COLUMN latency_ms INTEGER,
    ADD COLUMN failure_code VARCHAR(40);
