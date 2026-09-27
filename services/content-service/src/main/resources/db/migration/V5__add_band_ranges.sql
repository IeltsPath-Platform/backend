-- IELTS band range a topic or knowledge point is meant for. NULL at either end means open; both NULL means every
-- band, which keeps existing content in every learner's path. A knowledge point with its own range replaces its
-- topic's range as a whole (see BandRange.orInherit).
ALTER TABLE topics
    ADD COLUMN band_min NUMERIC(2,1),
    ADD COLUMN band_max NUMERIC(2,1),
    ADD CONSTRAINT chk_topics_band_min CHECK (band_min IS NULL OR (band_min BETWEEN 0 AND 9 AND band_min * 2 = trunc(band_min * 2))),
    ADD CONSTRAINT chk_topics_band_max CHECK (band_max IS NULL OR (band_max BETWEEN 0 AND 9 AND band_max * 2 = trunc(band_max * 2))),
    ADD CONSTRAINT chk_topics_band_order CHECK (band_min IS NULL OR band_max IS NULL OR band_min <= band_max);

ALTER TABLE knowledge_points
    ADD COLUMN band_min NUMERIC(2,1),
    ADD COLUMN band_max NUMERIC(2,1),
    ADD CONSTRAINT chk_knowledge_points_band_min CHECK (band_min IS NULL OR (band_min BETWEEN 0 AND 9 AND band_min * 2 = trunc(band_min * 2))),
    ADD CONSTRAINT chk_knowledge_points_band_max CHECK (band_max IS NULL OR (band_max BETWEEN 0 AND 9 AND band_max * 2 = trunc(band_max * 2))),
    ADD CONSTRAINT chk_knowledge_points_band_order CHECK (band_min IS NULL OR band_max IS NULL OR band_min <= band_max);
