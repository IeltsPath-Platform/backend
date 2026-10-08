-- The LLM now explains its grade: per-criterion comments, an overall summary and what to study next, kept as JSON
-- beside the band so the placement report can show them. Jobs graded before this, or by the default band, have none.
ALTER TABLE grading_jobs ADD COLUMN llm_feedback JSONB;
