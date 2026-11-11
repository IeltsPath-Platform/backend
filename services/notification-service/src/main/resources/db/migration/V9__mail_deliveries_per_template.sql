-- One email per source event and template: a single event can now produce more than one template later.
-- Existing rows were all password-reset mails, so they take that template name.
ALTER TABLE mail_deliveries ADD COLUMN template VARCHAR(64) NOT NULL DEFAULT 'password-reset';
ALTER TABLE mail_deliveries ALTER COLUMN template DROP DEFAULT;
ALTER TABLE mail_deliveries DROP CONSTRAINT mail_deliveries_pkey;
ALTER TABLE mail_deliveries ADD CONSTRAINT mail_deliveries_pkey PRIMARY KEY (event_id, template);
