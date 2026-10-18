ALTER TABLE grading_jobs ADD COLUMN prompt_version VARCHAR(100);
ALTER TABLE essay_grade_comparisons ALTER COLUMN llm_prompt_version TYPE VARCHAR(120);

CREATE TABLE ai_gold_essays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1','TASK_2')),
    prompt TEXT NOT NULL,
    chart_facts TEXT,
    essay TEXT NOT NULL,
    examiner_band NUMERIC(2,1) NOT NULL CHECK (examiner_band BETWEEN 0 AND 9 AND examiner_band * 2 = floor(examiner_band * 2)),
    examiner_criteria JSONB NOT NULL DEFAULT '[]'::jsonb,
    examiner_error_codes VARCHAR(60)[] NOT NULL DEFAULT '{}',
    source VARCHAR(20) NOT NULL CHECK (source IN ('COMPARISON','MANUAL')),
    comparison_submission_id UUID UNIQUE REFERENCES learner_submissions(id),
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_gold_essays_task ON ai_gold_essays(task, created_at, id);

CREATE TABLE ai_eval_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task VARCHAR(10) NOT NULL CHECK (task IN ('TASK_1','TASK_2')),
    model VARCHAR(200) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('RUNNING','DONE','FAILED')),
    n INTEGER NOT NULL DEFAULT 0 CHECK (n >= 0),
    within_half_band NUMERIC(6,5),
    mae NUMERIC(6,4),
    qwk NUMERIC(8,6),
    per_criterion JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_code_false_positive NUMERIC(6,5),
    passed BOOLEAN,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    failure_code VARCHAR(50)
);

CREATE TABLE ai_eval_items (
    run_id UUID NOT NULL REFERENCES ai_eval_runs(id) ON DELETE CASCADE,
    gold_id UUID NOT NULL REFERENCES ai_gold_essays(id),
    llm_band NUMERIC(2,1) CHECK (llm_band BETWEEN 0 AND 9),
    llm_criteria JSONB,
    llm_error_codes VARCHAR(60)[],
    diff JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (run_id, gold_id)
);

CREATE TABLE ai_prompt_activations (
    task VARCHAR(10) PRIMARY KEY CHECK (task IN ('TASK_1','TASK_2')),
    prompt_version VARCHAR(100) NOT NULL,
    run_id UUID NOT NULL REFERENCES ai_eval_runs(id),
    activated_by UUID NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_eval_daily_usage (
    usage_date DATE PRIMARY KEY,
    units INTEGER NOT NULL CHECK (units >= 0)
);
