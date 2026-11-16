-- Demo vocabulary notebook for the demo learner (placeholder demoUserId: h2 unless DEMO_LEARNER_ID names another account),
-- written after every Flyway migrate when demo data is enabled (DEMO_DATA_ENABLED=true, Flyway placeholder demoData).
-- DEV/DEMO ONLY. Idempotent: a word the learner already saved is skipped, so progress made since is never overwritten.
--
-- 36 saved words from the seeded catalog, spread over every understanding level the notebook shows:
--    8 mastered (box 5, next review in 10 days)         4 learning, due now (box 3 and 4)    4 learning, due in 2 days
--    6 familiar, due now (box 2)                        4 fallen back to box 1 after reviews, due now
--    6 new, never reviewed (box 1, not due)             4 familiar, due tomorrow
-- That is 14 learned words due for review, so the daily review reminder applies. Plus review events for the last week
-- (the weekly goal chart and today's count), the vocabulary study profile, streak and XP.

INSERT INTO flashcards (id, user_id, source_type, vocabulary_sense_id, source_reference_id, front, back, status, created_at,
                        updated_at, leitner_box, next_review_at, last_reviewed_at, review_count)
SELECT md5('${demoUserId}:demo:flashcard:' || p.sense_id)::uuid, '${demoUserId}'::uuid, 'VOCABULARY_SENSE', p.sense_id,
       p.sense_id, p.lemma, p.meaning, 'ACTIVE', now() - (14 - p.rn / 3) * interval '1 day', now() - interval '3 hours',
       CASE WHEN p.rn <= 8 THEN 5 WHEN p.rn <= 12 THEN 3 WHEN p.rn <= 16 THEN 4 WHEN p.rn <= 22 THEN 2
            WHEN p.rn <= 28 THEN 1 WHEN p.rn <= 32 THEN 1 ELSE 2 END,
       CASE WHEN p.rn <= 8 THEN now() + interval '10 days'
            WHEN p.rn <= 12 THEN now() - p.rn * interval '1 hour'
            WHEN p.rn <= 16 THEN now() + interval '2 days'
            WHEN p.rn <= 22 THEN now() - interval '1 day'
            WHEN p.rn <= 28 THEN NULL
            WHEN p.rn <= 32 THEN now() - interval '6 hours'
            ELSE now() + interval '1 day' END,
       CASE WHEN p.rn <= 28 AND p.rn > 22 THEN NULL ELSE now() - (p.rn % 7 + 1) * interval '1 day' END,
       CASE WHEN p.rn <= 8 THEN 6 WHEN p.rn <= 16 THEN 4 WHEN p.rn <= 22 THEN 2 WHEN p.rn <= 28 THEN 0
            WHEN p.rn <= 32 THEN 2 ELSE 1 END
FROM (SELECT (row_number() OVER (ORDER BY md5(s.id::text)))::int AS rn, s.id AS sense_id, i.lemma,
             s.vietnamese_meaning AS meaning
      FROM (SELECT s2.* FROM vocabulary_senses s2 WHERE s2.status = 'ACTIVE' AND s2.vietnamese_meaning IS NOT NULL
                  AND s2.vietnamese_meaning <> '' ORDER BY md5(s2.id::text) LIMIT 36) s
      JOIN vocabulary_items i ON i.id = s.vocabulary_item_id AND i.status = 'ACTIVE') p
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

-- Review events over the last seven days, about a third of the reviewed cards each day, a few wrong.
INSERT INTO flashcard_review_events (id, user_id, flashcard_id, vocabulary_sense_id, session_id, correct, reviewed_at)
SELECT md5('${demoUserId}:demo:review-event:' || f.id || ':' || d.day)::uuid, f.user_id, f.id, f.vocabulary_sense_id,
       md5('${demoUserId}:demo:review-session:' || d.day)::uuid, (abs(hashtext(f.id::text)) + d.day) % 5 <> 0,
       now() - d.day * interval '1 day' - ((abs(hashtext(f.id::text)) % 40) + 5) * interval '1 minute'
FROM flashcards f
CROSS JOIN generate_series(0, 6) AS d(day)
WHERE f.user_id = '${demoUserId}'::uuid AND f.review_count > 0 AND '${demoData}' = 'true'
  AND (abs(hashtext(f.id::text)) + d.day) % 3 = 0
ON CONFLICT DO NOTHING;

INSERT INTO vocabulary_weekly_goals (user_id, target_words, updated_at)
SELECT '${demoUserId}'::uuid, 120, now() WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO vocabulary_study_profiles (user_id, timezone, total_xp, level, hearts, max_hearts, hearts_regen_at, updated_at)
SELECT '${demoUserId}'::uuid, 'Asia/Ho_Chi_Minh', 640, 2, 4, 7, now() - interval '3 hours', now()
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO vocabulary_study_streaks (user_id, current_days, longest_days, last_active_date, streak_start, evaluated_through,
                                      freeze_available, updated_at)
SELECT '${demoUserId}'::uuid, 6, 6, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
       (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 5, (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, true, now()
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO vocabulary_study_xp_ledger (id, user_id, source, source_ref, amount, local_date, created_at)
SELECT md5('${demoUserId}:demo:vocabulary-xp:' || x.n)::uuid, '${demoUserId}'::uuid, x.source, 'demo:unit:' || x.n, x.amount,
       (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - x.day, now() - x.day * interval '1 day'
FROM (VALUES (1, 'UNIT_FIRST_COMPLETION', 20, 5), (2, 'UNIT_PERFECT_BONUS', 10, 5), (3, 'UNIT_FIRST_COMPLETION', 20, 3),
             (4, 'UNIT_FIRST_COMPLETION', 20, 1), (5, 'UNIT_PERFECT_BONUS', 10, 1)) AS x(n, source, amount, day)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;
