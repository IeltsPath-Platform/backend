CREATE INDEX idx_posts_search
    ON posts USING GIN (to_tsvector('simple', coalesce(title, '') || ' ' || body));
