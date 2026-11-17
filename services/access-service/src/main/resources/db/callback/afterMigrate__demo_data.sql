-- Demo points for learner 00000000-0000-0000-0000-000000000001 (user-service demo account), written after every
-- Flyway migrate when demo data is enabled (DEMO_DATA_ENABLED=true, Flyway placeholder demoData). DEV/DEMO ONLY.
-- 30 points = ten Writing gradings at 3 points each. Written once: later debits are not topped up again.

INSERT INTO point_wallets (user_id, balance, total_credited, total_debited)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, 30, 30, 0
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO point_ledger_entries (id, user_id, delta, balance_after, transaction_type, reference_type, reference_id,
                                  idempotency_key, description)
SELECT '00000000-0000-0000-0000-0000000000a1'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 30, 30,
       'ADMIN_ADJUSTMENT', 'DEMO_SEED', '00000000-0000-0000-0000-000000000001'::uuid, 'demo-seed:learner-1:points',
       'Demo points'
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

-- Demo activation keys with codes written here, so anyone can type them in "Kích hoạt key" to top a wallet up and try the
-- flow where a learner has too few points and must enter a key first. DEV/DEMO ONLY: the codes are public, which is why
-- they live here and not in a migration (a migration also runs in production).
--   B-2611-001  POINT_50   x10   IP-DEV5-KEYS-AAA2 .. IP-DEV5-KEYS-AAAB   (the 10th is already issued, given out directly)
--   B-2611-002  POINT_100  x5    IP-DEV9-KEYS-AAA2 .. IP-DEV9-KEYS-AAA6
-- The last character is the seq-th character of the key alphabet (no 0, 1, I, O). Written once; a key a learner has
-- activated stays activated.
INSERT INTO key_batches (code, label, product_id, quantity, expires_on)
SELECT batch.code, 'Đợt 11/2026 (demo)', product.id, batch.quantity, DATE '2027-12-31'
FROM (VALUES ('B-2611-001', 'POINT_50', 10), ('B-2611-002', 'POINT_100', 5)) AS batch(code, product_code, quantity)
JOIN key_products product ON product.code = batch.product_code
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO activation_keys (product_id, code_hash, code_hint, status, expires_at, batch_code, seq, issued_at, note)
SELECT product.id, encode(digest(demo.code, 'sha256'), 'hex'), right(demo.code, 4), 'ACTIVE',
       (DATE '2027-12-31' + 1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh', demo.batch_code, demo.seq,
       CASE WHEN demo.batch_code = 'B-2611-001' AND demo.seq = 10 THEN now() END,
       CASE WHEN demo.batch_code = 'B-2611-001' AND demo.seq = 10 THEN 'Demo: giao trực tiếp' END
FROM (SELECT batch.code AS batch_code, batch.product_code, series.seq,
             batch.prefix || substr('23456789ABCDEFGHJKLMNPQRSTUVWXYZ', series.seq, 1) AS code
      FROM (VALUES ('B-2611-001', 'POINT_50', 'IP-DEV5-KEYS-AAA', 10),
                   ('B-2611-002', 'POINT_100', 'IP-DEV9-KEYS-AAA', 5)) AS batch(code, product_code, prefix, quantity)
      CROSS JOIN LATERAL generate_series(1, batch.quantity) AS series(seq)) demo
JOIN key_products product ON product.code = demo.product_code
WHERE '${demoData}' = 'true'
  AND EXISTS (SELECT 1 FROM key_batches existing WHERE existing.code = demo.batch_code)
ON CONFLICT DO NOTHING;

-- Demo learner for the IELTS 5.0 demo (placeholder demoUserId: h2 unless DEMO_LEARNER_ID names another account):
-- Premium for six months with three human gradings, and a wallet that went 1000 -> 975 points after a few gradings and
-- course unlocks. Written once: later debits are not topped up again. DEV/DEMO ONLY.
INSERT INTO subscriptions (id, user_id, plan_id, status, starts_at, ends_at, source_type, human_grading_credits_total,
                           human_grading_credits_used)
SELECT md5('${demoUserId}:demo:subscription')::uuid, '${demoUserId}'::uuid, '22222222-2222-2222-2222-222222222222'::uuid,
       'ACTIVE', now() - interval '15 days', now() + interval '165 days', 'ADMIN', 3, 1
WHERE '${demoData}' = 'true'
  AND NOT EXISTS (SELECT 1 FROM subscriptions s WHERE s.user_id = '${demoUserId}'::uuid AND s.status = 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO point_wallets (user_id, balance, total_credited, total_debited)
SELECT '${demoUserId}'::uuid, 975, 1100, 125
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;

INSERT INTO point_ledger_entries (id, user_id, delta, balance_after, transaction_type, reference_type, reference_id,
                                  idempotency_key, description, created_at)
SELECT md5('${demoUserId}:demo:ledger:' || e.n)::uuid, '${demoUserId}'::uuid, e.delta, e.balance_after, e.type, e.ref_type,
       md5('${demoUserId}:demo:ledger-ref:' || e.n), 'demo-seed:' || '${demoUserId}' || ':ledger:' || e.n, e.note,
       now() - e.age
FROM (VALUES (1, 1000, 1000, 'KEY_CREDIT', 'ACTIVATION_KEY', 'Kích hoạt key POINT_100 x10 (demo)', interval '15 days'),
             (2, -60, 940, 'AI_GRADING_DEBIT', 'WRITING_SUBMISSION', 'Chấm Writing bằng AI (3 bài)', interval '12 days'),
             (3, -50, 890, 'COURSE_UNLOCK_DEBIT', 'COURSE', 'Mở khóa kỹ năng khóa IELTS 5.0', interval '14 days'),
             (4, 100, 990, 'LEARNING_REWARD', 'DAILY_QUEST', 'Thưởng nhiệm vụ ngày', interval '5 days'),
             (5, -15, 975, 'HEART_REFILL_DEBIT', 'HEARTS', 'Hồi tim', interval '2 days'))
     AS e(n, delta, balance_after, type, ref_type, note, age)
WHERE '${demoData}' = 'true'
ON CONFLICT DO NOTHING;
