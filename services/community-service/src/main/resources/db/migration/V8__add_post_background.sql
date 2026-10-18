ALTER TABLE posts ADD COLUMN background_style VARCHAR(16);

ALTER TABLE posts ADD CONSTRAINT posts_background_style_check
    CHECK (background_style IS NULL OR background_style IN
        ('VIOLET','CRIMSON','MIDNIGHT','SUNSET','OCEAN','FOREST','ROSE','AURORA'));

ALTER TABLE posts ADD CONSTRAINT posts_background_text_only_check
    CHECK (background_style IS NULL
        OR (jsonb_array_length(image_urls) = 0 AND video_url IS NULL AND char_length(body) <= 280));
