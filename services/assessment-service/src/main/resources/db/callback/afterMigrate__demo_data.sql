-- Demo examiner review for the demo learner (placeholder demoUserId: h2 unless DEMO_LEARNER_ID names another account),
-- written after every Flyway migrate when demo data is enabled (DEMO_DATA_ENABLED=true, Flyway placeholder demoData).
-- DEV/DEMO ONLY. Idempotent: every row has a fixed id.
--
-- The learner used a Premium human grading credit on the practice essay of lesson 11 (the essay itself, the request and
-- its outcome are written by the learning callback with the same ids). The examiner, user-service demo account
-- 00000000-0000-0000-0000-000000000004 (examiner@ielts.demo), finished it a day ago: band 6.0, four criterion comments,
-- three marked errors and written feedback.

INSERT INTO human_review_submissions (id, request_id, learner_id, source_submission_id, source, task, prompt_snapshot,
                                      essay_text, rubric_snapshot, band, criteria, error_codes, feedback, created_at,
                                      completed_at, annotations)
SELECT md5('${demoUserId}:demo:human-review')::uuid, md5('${demoUserId}:demo:human-review-request')::uuid,
       '${demoUserId}'::uuid, md5('${demoUserId}:demo:essay:practice-item')::uuid, 'PRACTICE', 'TASK_2', e.prompt::jsonb, e.text,
       '{"name":"IELTS Writing Task 2","criteria":["TR","CC","LR","GRA"]}'::jsonb, 6.0,
       '[{"criterion":"TR","band":6.0,"comment":"Bạn trả lời đủ câu hỏi và giữ quan điểm nhất quán. Mỗi lý do nên có thêm một ví dụ cụ thể."},
         {"criterion":"CC","band":6.0,"comment":"Bố cục bốn đoạn rõ ràng, từ nối dùng đúng chỗ."},
         {"criterion":"LR","band":6.0,"comment":"Từ vựng chính xác, chỉ lặp lại green roofs nhiều lần. Thử dùng rooftop gardens hoặc planted roofs."},
         {"criterion":"GRA","band":6.0,"comment":"Ít lỗi cơ bản. Thử thêm câu phức với mệnh đề quan hệ để lên band 6.5."}]'::jsonb,
       ARRAY['COLLOCATION', 'GRAMMAR_OTHER'],
       'Bài viết rõ ràng và ít lỗi. Để đạt band 6.5, hãy phát triển mỗi lý do bằng một ví dụ cụ thể và dùng thêm câu phức. Bài hiện còn ngắn hơn 250 từ.',
       now() - interval '30 hours', now() - interval '1 day',
       jsonb_build_array(
           jsonb_build_object('excerpt', 'takes in rainwater', 'start', position('takes in rainwater' in e.text) - 1,
                              'end', position('takes in rainwater' in e.text) - 1 + length('takes in rainwater'),
                              'errorCode', 'COLLOCATION', 'suggestion', 'absorbs rainwater'),
           jsonb_build_object('excerpt', 'cost a lot of money', 'start', position('cost a lot of money' in e.text) - 1,
                              'end', position('cost a lot of money' in e.text) - 1 + length('cost a lot of money'),
                              'errorCode', 'COLLOCATION', 'suggestion', 'are expensive to install'),
           jsonb_build_object('excerpt', 'the streets nearby stay cooler', 'start', position('the streets nearby stay cooler' in e.text) - 1,
                              'end', position('the streets nearby stay cooler' in e.text) - 1 + length('the streets nearby stay cooler'),
                              'errorCode', 'GRAMMAR_OTHER', 'suggestion', 'the nearby streets stay cooler'))
FROM (VALUES (
$essay$I partly agree that new buildings should have green roofs. Green roofs help the city in many ways, but making them compulsory for every building is too strict.

The main benefit is that a green roof keeps a building cool, so people use less electricity for air conditioning. It also takes in rainwater, which reduces flooding in heavy storms. For example, many offices in Singapore have rooftop gardens and the streets nearby stay cooler.

On the other hand, green roofs cost a lot of money and need regular care. Small homeowners may not be able to pay for them, and some old roofs cannot carry the weight of soil and plants.

In conclusion, I think only large buildings should be required to have green roofs, and the government should offer help to smaller owners.$essay$,
$prompt$
{"questionVersionId":"21000000-0000-4000-8000-070000000001","stem":"Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.","task":"TASK_2","minWords":250,"passBand":6,"chartFacts":null,"sampleAnswer":null,"images":[],"knowledgePointIds":["21000000-0000-4000-8000-020000000007"]}$prompt$)
) AS e(text, prompt)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status, point_cost_snapshot, point_ledger_entry_id,
                          premium_subscription_id, idempotency_key, created_at, completed_at, processing_started_at,
                          assigned_examiner_id, claimed_at, reason, human_review_submission_id, requested_examiner_id,
                          requested_at, examiner_changes)
SELECT md5('${demoUserId}:demo:grading-job:examiner')::uuid, NULL, '${demoUserId}'::uuid, 'WRITING', 'HUMAN', 'COMPLETED',
       NULL, NULL, md5('${demoUserId}:demo:subscription')::uuid, 'demo-seed:${demoUserId}:human-review',
       now() - interval '30 hours', now() - interval '1 day', now() - interval '28 hours',
       '00000000-0000-0000-0000-000000000004'::uuid, now() - interval '28 hours', 'HUMAN_CREDIT',
       md5('${demoUserId}:demo:human-review')::uuid, '00000000-0000-0000-0000-000000000004'::uuid,
       now() - interval '30 hours', 0
WHERE '${demoData}' = 'true'
  AND EXISTS (SELECT 1 FROM human_review_submissions h WHERE h.id = md5('${demoUserId}:demo:human-review')::uuid)
ON CONFLICT DO NOTHING;

INSERT INTO human_reviews (id, grading_job_id, grader_user_id, status, assigned_at, started_at, completed_at, reviewer_note)
SELECT md5('${demoUserId}:demo:human-review-row')::uuid, md5('${demoUserId}:demo:grading-job:examiner')::uuid,
       '00000000-0000-0000-0000-000000000004'::uuid, 'COMPLETED', now() - interval '28 hours', now() - interval '27 hours',
       now() - interval '1 day', 'Bài ít lỗi, cần phát triển ý thêm.'
WHERE '${demoData}' = 'true'
  AND EXISTS (SELECT 1 FROM grading_jobs j WHERE j.id = md5('${demoUserId}:demo:grading-job:examiner')::uuid)
ON CONFLICT DO NOTHING;
