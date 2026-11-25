-- An optional Writing review can now review the knowledge point of the most frequent error of a weak criterion instead of
-- the criterion's own point. The review keeps the criterion's point here, so an examiner's result still finds the review
-- (to make it blocking or close it) and a criterion has one waiting review. Null for every review made before, and for any
-- review that reviews the criterion's own point: those match on knowledge_point_id as before.
ALTER TABLE review_items ADD COLUMN criterion_knowledge_point_id uuid;

CREATE INDEX idx_review_items_pending_criterion_kp ON review_items (user_id, criterion_knowledge_point_id)
    WHERE status = 'PENDING' AND criterion_knowledge_point_id IS NOT NULL;
