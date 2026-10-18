-- DRAFT distractor mapping for learning Reading and Listening multiple-choice questions, pending review by an IELTS expert.
-- Only learning questions with an explanation that names why a specific wrong option is wrong are mapped.
-- Each row is guarded by the correct key, so a mapping that no longer matches its question changes nothing.
-- Adding a key does not change which option is correct, so already published versions are not re-graded.
-- Wrong options with no fitting code in the taxonomy are left unmapped; see phase-03 for the gaps to review.

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '23000000-0000-4000-8000-070000000006' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '23000000-0000-4000-8000-070000000011' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '23000000-0000-4000-8000-070000000015' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "DISTRACTOR_CORRECTED", "B": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '23000000-0000-4000-8000-070000000026' AND answer_spec->>'correct' = 'C';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '24000000-0000-4000-8000-070000000002' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '27000000-0000-4000-8000-070000000014' AND answer_spec->>'correct' = 'A';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '33000000-0000-4000-8000-070000000001' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "OFF_TOPIC"}}'::jsonb
WHERE id = '26000000-0000-4000-8000-070000000001' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"B": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '26000000-0000-4000-8000-070000000003' AND answer_spec->>'correct' = 'C';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "KEYWORD_MATCH_ONLY", "C": "KEYWORD_MATCH_ONLY"}}'::jsonb
WHERE id = '26000000-0000-4000-8000-070000000013' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"B": "WRONG_PARAGRAPH"}}'::jsonb
WHERE id = '26000000-0000-4000-8000-070000000015' AND answer_spec->>'correct' = 'C';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "WRONG_PARAGRAPH"}}'::jsonb
WHERE id = '27000000-0000-4000-8000-070000000010' AND answer_spec->>'correct' = 'A';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '27000000-0000-4000-8000-070000000012' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "INFERENCE_UNSUPPORTED", "C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '27000000-0000-4000-8000-070000000013' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"B": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '27000000-0000-4000-8000-070000000015' AND answer_spec->>'correct' = 'C';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '33000000-0000-4000-8000-070000000002' AND answer_spec->>'correct' = 'A';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "SCOPE_OVERGENERALISED", "C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '33000000-0000-4000-8000-070000000005' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"A": "PARAPHRASE_TRAP", "C": "INFERENCE_UNSUPPORTED"}}'::jsonb
WHERE id = '33000000-0000-4000-8000-070000000008' AND answer_spec->>'correct' = 'B';

UPDATE question_versions
SET answer_spec = answer_spec || '{"distractors": {"B": "NUMBER_FORMAT", "C": "OFF_TOPIC"}}'::jsonb
WHERE id = '33000000-0000-4000-8000-070000000007' AND answer_spec->>'correct' = 'A';
