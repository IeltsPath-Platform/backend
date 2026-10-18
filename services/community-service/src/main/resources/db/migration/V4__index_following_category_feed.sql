-- Supports keyset reads for the intersection of followed authors and a selected category.
CREATE INDEX idx_posts_author_category_feed
    ON posts(author_id, category, created_at DESC, id DESC)
    WHERE status = 'ACTIVE';
