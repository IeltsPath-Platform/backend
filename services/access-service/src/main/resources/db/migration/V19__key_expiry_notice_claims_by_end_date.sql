-- A claim now belongs to one end date, so a subscription that is renewed or extended is announced again for its new end.
ALTER TABLE subscription_expiry_notices ADD COLUMN ends_at TIMESTAMPTZ;

-- The scan has never been scheduled, so there are normally no rows; any row gets the end date the subscription has now.
UPDATE subscription_expiry_notices AS notice
SET ends_at = COALESCE((SELECT s.ends_at FROM subscriptions s WHERE s.id = notice.subscription_id), notice.sent_at);

ALTER TABLE subscription_expiry_notices ALTER COLUMN ends_at SET NOT NULL;
ALTER TABLE subscription_expiry_notices DROP CONSTRAINT subscription_expiry_notices_pkey;
ALTER TABLE subscription_expiry_notices ADD PRIMARY KEY (subscription_id, stage, ends_at);
