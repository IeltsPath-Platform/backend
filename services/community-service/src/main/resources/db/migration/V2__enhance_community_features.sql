ALTER TABLE posts ADD COLUMN image_urls JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE posts ADD COLUMN video_url VARCHAR(500);
ALTER TABLE posts ADD COLUMN target_band NUMERIC(2,1) CHECK (target_band BETWEEN 0 AND 9 AND mod(target_band, 0.5) = 0);
ALTER TABLE posts ADD CONSTRAINT posts_images_check CHECK (jsonb_typeof(image_urls) = 'array' AND jsonb_array_length(image_urls) <= 4);
ALTER TABLE posts DROP CONSTRAINT posts_category_check;
ALTER TABLE posts ADD CONSTRAINT posts_category_check CHECK (category IN ('GENERAL','QUESTION','DISCUSSION','WRITING_SHARING','SPEAKING_PARTNER','RESOURCE_TIPS'));
ALTER TABLE post_reactions DROP CONSTRAINT post_reactions_reaction_type_check;
ALTER TABLE post_reactions ADD CONSTRAINT post_reactions_reaction_type_check CHECK (reaction_type IN ('LIKE','LOVE','HELPFUL','ACCURATE'));
-- Archive displaced rows before enforcing one reaction per learner and post.
CREATE TABLE post_reaction_archive AS
SELECT post_id, user_id, reaction_type, created_at, CURRENT_TIMESTAMP AS archived_at
FROM (SELECT *, row_number() OVER (PARTITION BY post_id,user_id ORDER BY created_at DESC,reaction_type DESC) AS position FROM post_reactions) ranked WHERE position > 1;
DELETE FROM post_reactions r USING post_reaction_archive a WHERE r.post_id=a.post_id AND r.user_id=a.user_id AND r.reaction_type=a.reaction_type;
ALTER TABLE post_reactions DROP CONSTRAINT post_reactions_pkey;
ALTER TABLE post_reactions ADD PRIMARY KEY (post_id,user_id);
CREATE TABLE user_follows (follower_id UUID NOT NULL, followee_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(follower_id,followee_id), CHECK(follower_id <> followee_id));
CREATE INDEX idx_user_follows_followee ON user_follows(followee_id,created_at DESC,follower_id DESC);
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id,created_at DESC,followee_id DESC);
CREATE TABLE post_bookmarks (user_id UUID NOT NULL, post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(user_id,post_id));
CREATE INDEX idx_post_bookmarks_user ON post_bookmarks(user_id,created_at DESC,post_id DESC);
CREATE INDEX idx_posts_category_feed ON posts(category,created_at DESC,id DESC) WHERE status='ACTIVE';
CREATE INDEX idx_posts_author_feed ON posts(author_id,created_at DESC,id DESC) WHERE status='ACTIVE';
