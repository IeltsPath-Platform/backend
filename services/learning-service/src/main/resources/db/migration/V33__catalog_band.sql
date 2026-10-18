-- The course band of each knowledge point, kept next to its skill so a scheduled job (no learner request, so no gateway
-- token for Content) can still rate a learner. It is filled the next time a learner opens the course list.
ALTER TABLE knowledge_point_catalog ADD COLUMN band NUMERIC(3, 1);
