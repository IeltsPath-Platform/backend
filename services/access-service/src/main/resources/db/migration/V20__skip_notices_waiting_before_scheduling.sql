-- No scheduled job ran in access-service before this release, so everything still waiting to be sent is old. As in V6,
-- none of it is sent: learners only hear about what happens from now on.
UPDATE outbox_events SET status = 'NOT_RELAYED' WHERE status = 'PENDING';

-- Subscriptions that have already ended get no expiry notice either.
INSERT INTO subscription_expiry_notices (subscription_id, stage, ends_at, sent_at)
SELECT id, 'EXPIRED', ends_at, CURRENT_TIMESTAMP FROM subscriptions WHERE ends_at IS NOT NULL AND ends_at <= CURRENT_TIMESTAMP
ON CONFLICT (subscription_id, stage, ends_at) DO NOTHING;
