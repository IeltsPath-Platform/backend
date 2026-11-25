-- Demo notifications for the demo learner (placeholder demoUserId: h2 unless DEMO_LEARNER_ID names another account),
-- written after every Flyway migrate when demo data is enabled (DEMO_DATA_ENABLED=true, Flyway placeholder demoData).
-- DEV/DEMO ONLY. Idempotent: the source event id is fixed per notice.
-- Four notices: unread (words due for review today, the examiner finished the practice essay) and read (placement result
-- ready, Premium activated). The grading notice points at the grading job the assessment callback writes.
INSERT INTO notifications (id, source_event_id, recipient_id, actor_id, event_type, post_id, comment_id, created_at,
                           read_at, grading_job_id)
SELECT md5('${demoUserId}:demo:notice:' || n.code)::uuid, md5('${demoUserId}:demo:notice-event:' || n.code)::uuid,
       '${demoUserId}'::uuid, '${demoUserId}'::uuid, n.event_type, NULL, NULL, now() - n.age,
       CASE WHEN n.read THEN now() - n.age + interval '10 minutes' END,
       CASE WHEN n.job IS NOT NULL THEN md5('${demoUserId}:demo:grading-job:' || n.job)::uuid END
FROM (VALUES
    ('review-due', 'library.vocabulary-review-due.v1', interval '2 hours', false, NULL::text),
    ('grading-completed', 'assessment.grading-completed.v1', interval '1 day', false, 'examiner'),
    ('result-ready', 'assessment.result-ready.v1', interval '16 days', true, NULL),
    ('subscription-granted', 'access.subscription-granted.v1', interval '15 days', true, NULL)
) AS n(code, event_type, age, read, job)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;
