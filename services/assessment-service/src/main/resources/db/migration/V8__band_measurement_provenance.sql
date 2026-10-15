ALTER TABLE assessment_results ADD COLUMN band_snapshot JSONB;
ALTER TABLE skill_scores ADD COLUMN grader_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE grading_jobs ADD COLUMN measurement_quality VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN_LEGACY'
 CHECK (measurement_quality IN ('UNKNOWN_LEGACY','MEASURED','FALLBACK'));
CREATE INDEX idx_assessment_attempts_band_range ON assessment_attempts(user_id,attempt_type,submitted_at,id);
CREATE INDEX idx_assessment_results_completed_version ON assessment_results(attempt_id,result_version DESC) WHERE status='COMPLETED';
