CREATE TABLE community_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    content_type VARCHAR(16) NOT NULL CHECK (content_type IN ('POST','COMMENT')),
    content_id UUID NOT NULL,
    reason VARCHAR(1000) NOT NULL CHECK (length(btrim(reason)) BETWEEN 1 AND 1000),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DISMISSED','ACTIONED')),
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    CHECK ((status='PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR (status<>'PENDING' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)),
    CONSTRAINT uq_community_reports_reporter_target UNIQUE (reporter_id,content_type,content_id)
);
CREATE INDEX idx_community_reports_queue ON community_reports(status,created_at,id);
