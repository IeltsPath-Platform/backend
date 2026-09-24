-- Store the entitlement required by each resource instead of a Content-owned plan tier.
ALTER TABLE content_packages
    ADD COLUMN IF NOT EXISTS required_feature_key VARCHAR(100);
UPDATE content_packages
SET required_feature_key = CASE WHEN access_level = 'PREMIUM' THEN 'PREMIUM_CONTENT' ELSE NULL END;
ALTER TABLE content_packages
    DROP COLUMN IF EXISTS access_level;

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS required_feature_key VARCHAR(100);
UPDATE questions
SET required_feature_key = CASE WHEN access_level = 'PREMIUM' THEN 'PREMIUM_CONTENT' ELSE NULL END;
ALTER TABLE questions
    DROP COLUMN IF EXISTS access_level;

ALTER TABLE learning_videos
    ADD COLUMN IF NOT EXISTS required_feature_key VARCHAR(100);
UPDATE learning_videos
SET required_feature_key = CASE WHEN access_level = 'PREMIUM' THEN 'VIDEO_LEARNING_PREMIUM' ELSE NULL END;
ALTER TABLE learning_videos
    DROP COLUMN IF EXISTS access_level;
