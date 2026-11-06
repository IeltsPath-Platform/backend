-- DEV ONLY. Gives one learner a Speaking study history (answers with error tags) so there is something to read back as
-- "where this learner is weak". Never run it on a real environment: it writes answers the learner did not give.
--
-- Needs content-service migration V115 (the question and knowledge point ids below come from it).
--
--   psql -U postgres -d learning_db -v user_id=<learner uuid> -f scripts/dev/seed-speaking-learner-history.sql
--
-- It writes kp_evidence and question_attempt_events only. Mastery profiles are built from the evidence by the learning
-- service itself: restart learning-service (learning.mastery.backfill-on-startup), open the learning path once so the
-- catalog is refreshed, restart again, then run this script a second time; the last statement then fills top_error_codes
-- from the events with the rule of JdbcQuestionEventLog.statsByKnowledgePoint (top 3 codes over the last 90 days).
-- Safe to run again.
BEGIN;

CREATE TEMP TABLE seed_ev (k int, n int, kp_id uuid, correct boolean, src text, days_ago int, error_code text) ON COMMIT DROP;
INSERT INTO seed_ev VALUES
 -- Fluency and coherence: weak (1 right of 5)
 (2,1,'73000000-0000-4000-8000-020000000009', false,'lesson_exercise',12,'SPK_FILLER_OVERUSE'),
 (2,2,'73000000-0000-4000-8000-020000000009', false,'lesson_exercise',12,'SPK_WRONG_LINKER'),
 (2,3,'73000000-0000-4000-8000-020000000009', false,'practice_set',8,'SPK_WRONG_LINKER'),
 (2,4,'73000000-0000-4000-8000-020000000009', true ,'practice_set',6,NULL),
 (2,5,'73000000-0000-4000-8000-020000000009', false,'practice_set',3,'SPK_NO_LINKER'),
 -- Grammatical range: weak (2 right of 5)
 (4,1,'73000000-0000-4000-8000-020000000013', true ,'lesson_exercise',12,NULL),
 (4,2,'73000000-0000-4000-8000-020000000013', false,'lesson_exercise',12,'TENSE'),
 (4,3,'73000000-0000-4000-8000-020000000013', false,'practice_set',8,'TENSE'),
 (4,4,'73000000-0000-4000-8000-020000000013', true ,'practice_set',5,NULL),
 (4,5,'73000000-0000-4000-8000-020000000013', false,'practice_set',2,'SPK_SIMPLE_STRUCTURES_ONLY'),
 -- Speaking vocabulary: strong (4 right of 5)
 (3,1,'73000000-0000-4000-8000-020000000011', true ,'lesson_exercise',12,NULL),
 (3,2,'73000000-0000-4000-8000-020000000011', true ,'lesson_exercise',12,NULL),
 (3,3,'73000000-0000-4000-8000-020000000011', true ,'practice_set',8,NULL),
 (3,4,'73000000-0000-4000-8000-020000000011', false,'practice_set',5,'SPK_WRONG_COLLOCATION'),
 (3,5,'73000000-0000-4000-8000-020000000011', true ,'practice_set',2,NULL),
 -- Part 1 answers: strong (5 right of 5)
 (1,1,'73000000-0000-4000-8000-020000000007', true ,'lesson_exercise',11,NULL),
 (1,2,'73000000-0000-4000-8000-020000000007', true ,'lesson_exercise',11,NULL),
 (1,3,'73000000-0000-4000-8000-020000000007', true ,'practice_set',7,NULL),
 (1,4,'73000000-0000-4000-8000-020000000007', true ,'practice_set',4,NULL),
 (1,5,'73000000-0000-4000-8000-020000000007', true ,'practice_set',1,NULL);

INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, question_version_id, created_at)
SELECT ('7e000000-0000-4000-8000-0c' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid,
       :'user_id'::uuid, e.kp_id, e.correct, e.src,
       ('7e000000-0000-4000-8000-0d' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid,
       ('7e000000-0000-4000-8000-07' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid,
       now() - make_interval(days => e.days_ago)
  FROM seed_ev e ORDER BY e.days_ago DESC, e.k, e.n
ON CONFLICT DO NOTHING;

INSERT INTO question_attempt_events (id, user_id, question_version_id, knowledge_point_ids, correct, score, max_score,
                                     duration_ms, error_code, source, source_reference_id, first_submission, occurred_at)
SELECT ('7e000000-0000-4000-8000-0e' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid,
       :'user_id'::uuid, ('7e000000-0000-4000-8000-07' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid, ARRAY[e.kp_id],
       e.correct, CASE WHEN e.correct THEN 1 ELSE 0 END, 1, NULL, e.error_code, e.src,
       ('7e000000-0000-4000-8000-0d' || lpad((e.k * 10 + e.n)::text, 10, '0'))::uuid, true,
       now() - make_interval(days => e.days_ago)
  FROM seed_ev e ORDER BY e.days_ago DESC, e.k, e.n
ON CONFLICT DO NOTHING;

UPDATE kp_mastery_profiles m
   SET top_error_codes = COALESCE(r.codes, '{}'), updated_at = now()
  FROM (SELECT kp, array_agg(error_code ORDER BY n DESC, error_code) AS codes FROM (
          SELECT point AS kp, error_code, count(*) AS n,
                 row_number() OVER (PARTITION BY point ORDER BY count(*) DESC, error_code) AS rnk
            FROM (SELECT unnest(knowledge_point_ids) AS point, error_code FROM question_attempt_events
                   WHERE user_id = :'user_id'::uuid AND error_code IS NOT NULL
                     AND occurred_at >= now() - interval '90 days') ev
           GROUP BY point, error_code) ranked
         WHERE rnk <= 3 GROUP BY kp) r
 WHERE m.user_id = :'user_id'::uuid AND m.knowledge_point_id = r.kp;

COMMIT;
