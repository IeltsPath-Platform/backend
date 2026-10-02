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
