-- The learning goal an attempt was taken for is captured at start and never re-resolved later,
-- so historical and regraded results stay attributed to the goal they were performed under.
ALTER TABLE assessment_attempts ADD COLUMN learning_goal_id UUID;

-- Question to knowledge-point attribution copied from Content Service when the attempt starts.
-- Content remains canonical; this is the immutable snapshot the formal result is published with.
CREATE TABLE attempt_item_knowledge_points (
    attempt_item_id UUID NOT NULL REFERENCES attempt_items(id) ON DELETE RESTRICT,
    knowledge_point_id UUID NOT NULL,
    weight NUMERIC(5,2) NOT NULL CHECK (weight >= 0),
    PRIMARY KEY (attempt_item_id, knowledge_point_id)
);

ALTER TABLE item_results ADD COLUMN max_score NUMERIC(8,2) CHECK (max_score > 0);
ALTER TABLE item_results ADD CONSTRAINT chk_item_results_score_within_max
    CHECK (max_score IS NULL OR score <= max_score);

-- Explicit per-knowledge-point qualitative outcome recorded by the grader. An overall band never implies it.
CREATE TABLE item_result_knowledge_judgments (
    item_result_id UUID NOT NULL REFERENCES item_results(id) ON DELETE RESTRICT,
    knowledge_point_id UUID NOT NULL,
    judgment VARCHAR(20) NOT NULL CHECK (judgment IN ('PASS', 'FAIL', 'NOT_ASSESSED')),
    PRIMARY KEY (item_result_id, knowledge_point_id)
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;

-- A finalized result version is announced exactly once.
CREATE UNIQUE INDEX uq_outbox_assessment_completed_result
    ON outbox_events(aggregate_id) WHERE event_type = 'AssessmentCompleted.v2';
