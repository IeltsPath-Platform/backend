package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code conclusionKnowledgePointIds} is empty while PROBING. */
public record DiagnosticSession(UUID id, UUID userId, UUID rootReviewId, UUID rootKnowledgePointId, Status status,
                                List<UUID> conclusionKnowledgePointIds, Instant createdAt, Instant concludedAt) {
    public enum Status { PROBING, CONCLUDED }

    public boolean probing() { return status == Status.PROBING; }
}
