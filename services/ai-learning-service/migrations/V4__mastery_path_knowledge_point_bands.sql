-- Effective IELTS band of each knowledge point in a path, copied from Content Service when the path is built or
-- refreshed. DeepTutor's KnowledgePoint has no metadata field, and the Assessment consumer has no learner token to
-- read Content, so placement test-out reads the bands from here. NULL ends are open.
CREATE TABLE mastery_path_knowledge_point_bands (
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL,
    band_min NUMERIC(2,1) CHECK (band_min IS NULL OR band_min BETWEEN 0 AND 9),
    band_max NUMERIC(2,1) CHECK (band_max IS NULL OR band_max BETWEEN 0 AND 9),
    PRIMARY KEY (path_id, knowledge_point_id)
);
