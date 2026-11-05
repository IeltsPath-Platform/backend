ALTER TABLE content_assets
    ADD COLUMN access_tier VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE content_assets
    ADD CONSTRAINT content_assets_access_tier_check CHECK (access_tier IN ('PUBLIC', 'PREMIUM'));
