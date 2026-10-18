ALTER TABLE grading_jobs ADD COLUMN assigned_examiner_id UUID;
ALTER TABLE grading_jobs ADD COLUMN claimed_at TIMESTAMPTZ;
ALTER TABLE grading_jobs ADD COLUMN reason VARCHAR(30) CHECK (reason IN
    ('LLM_FAILED','LLM_QUOTA','LLM_NOT_CONFIGURED','COURSE_GATE_POLICY','HUMAN_CREDIT','QA_SAMPLE'));
UPDATE grading_jobs SET reason = 'HUMAN_CREDIT' WHERE grading_mode = 'HUMAN';
CREATE INDEX idx_grading_jobs_human_queue ON grading_jobs (grading_mode, status, created_at, id);

CREATE TABLE essay_grade_comparisons (
    submission_id UUID PRIMARY KEY REFERENCES learner_submissions(id),
    attempt_type VARCHAR(40) NOT NULL,
    task VARCHAR(10) NOT NULL,
    llm_band NUMERIC(2,1) CHECK (llm_band BETWEEN 0 AND 9),
    llm_criteria JSONB,
    llm_prompt_version VARCHAR(40),
    examiner_band NUMERIC(2,1) CHECK (examiner_band BETWEEN 0 AND 9),
    examiner_criteria JSONB,
    examiner_error_codes VARCHAR(60)[],
    examiner_id UUID,
    examiner_graded_at TIMESTAMPTZ
);

-- Pending course essays submitted before this policy still need an examiner.
INSERT INTO learner_submissions(id,user_id,attempt_item_id,prompt_snapshot,skill,text_payload,status,submission_key)
SELECT gen_random_uuid(),a.user_id,i.id,
       jsonb_build_object('stem',i.question_snapshot->>'stem','task',i.answer_snapshot->'answerSpec'->>'task','omitted',true),
       'WRITING','','SUBMITTED','course-omitted:'||i.id
FROM attempt_items i JOIN attempt_sections s ON s.id=i.attempt_section_id
JOIN assessment_attempts a ON a.id=s.attempt_id
WHERE a.attempt_type='COURSE_GATE' AND a.status='SUBMITTED'
  AND i.answer_snapshot->'answerSpec'->>'type'='ESSAY'
  AND NOT EXISTS(SELECT 1 FROM assessment_results r WHERE r.attempt_id=a.id AND r.status='COMPLETED')
  AND NOT EXISTS(SELECT 1 FROM learner_submissions l WHERE l.attempt_item_id=i.id AND l.user_id=a.user_id
      AND l.skill='WRITING' AND l.status='SUBMITTED' AND l.text_payload IS NOT NULL)
ON CONFLICT(user_id,submission_key) DO NOTHING;

INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status, idempotency_key, reason)
SELECT DISTINCT ON (l.attempt_item_id) gen_random_uuid(), l.id, l.user_id, l.skill, 'HUMAN', 'QUEUED',
       'gate-essay:' || l.attempt_item_id || ':human', 'COURSE_GATE_POLICY'
FROM learner_submissions l
JOIN attempt_items i ON i.id = l.attempt_item_id
JOIN attempt_sections s ON s.id = i.attempt_section_id
JOIN assessment_attempts a ON a.id = s.attempt_id
WHERE a.attempt_type = 'COURSE_GATE' AND a.status = 'SUBMITTED'
  AND l.skill='WRITING' AND l.status='SUBMITTED' AND l.text_payload IS NOT NULL
  AND i.answer_snapshot->'answerSpec'->>'type'='ESSAY'
  AND NOT EXISTS (SELECT 1 FROM assessment_results r WHERE r.attempt_id = a.id AND r.status = 'COMPLETED')
ORDER BY l.attempt_item_id,l.submitted_at DESC,l.id DESC
ON CONFLICT (idempotency_key) DO UPDATE SET reason = 'COURSE_GATE_POLICY';

INSERT INTO human_reviews (id, grading_job_id, status)
SELECT gen_random_uuid(), id, 'QUEUED' FROM grading_jobs WHERE grading_mode = 'HUMAN'
ON CONFLICT (grading_job_id) DO NOTHING;

-- Keep actual grades already available for pending course essays; their old prompt version is unknown.
INSERT INTO essay_grade_comparisons(submission_id,attempt_type,task,llm_band,llm_criteria)
SELECT DISTINCT ON (j.submission_id) j.submission_id,a.attempt_type,
       i.answer_snapshot->'answerSpec'->>'task',j.llm_band,COALESCE(j.llm_feedback->'criteria','[]'::jsonb)
FROM grading_jobs j JOIN learner_submissions l ON l.id=j.submission_id
JOIN attempt_items i ON i.id=l.attempt_item_id JOIN attempt_sections s ON s.id=i.attempt_section_id
JOIN assessment_attempts a ON a.id=s.attempt_id
WHERE a.attempt_type='COURSE_GATE' AND j.grading_mode='AI' AND j.status='COMPLETED' AND j.llm_band IS NOT NULL
  AND i.answer_snapshot->'answerSpec'->>'task' IN ('TASK_1','TASK_2')
ORDER BY j.submission_id,j.completed_at DESC NULLS LAST,j.id DESC
ON CONFLICT(submission_id) DO NOTHING;
