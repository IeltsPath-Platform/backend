package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code conclusionKnowledgePointIds} is empty while PROBING or when a conclusion is inconclusive. */
public record DiagnosticSession(UUID id, UUID userId, UUID rootReviewId, UUID rootKnowledgePointId, Status status,
                                List<UUID> conclusionKnowledgePointIds, ConclusionKind conclusionKind,
                                Instant createdAt, Instant concludedAt) {
    public enum Status { PROBING, CONCLUDED }
    public enum ConclusionKind { GAPS_FOUND, ROOT_IS_GAP, INCONCLUSIVE, NO_PREREQUISITES }

    public boolean probing() { return status == Status.PROBING; }
}
