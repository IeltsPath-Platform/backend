package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

/**
 * One short set on a prerequisite of the point under diagnosis. {@code NO_SET} means no set the learner has not seen
 * was left, so the result stays unknown. {@code correct}, {@code total} and {@code passed} are set once ANSWERED.
 */
public record DiagnosticProbe(UUID id, UUID sessionId, UUID knowledgePointId, UUID parentKnowledgePointId, int depth,
                              UUID packageId, UUID packageVersionId, Status status, Integer correct, Integer total,
                              Boolean passed, UUID requestId, Instant answeredAt) {
    public enum Status { OPEN, ANSWERED, NO_SET }

    public static DiagnosticProbe open(UUID sessionId, UUID knowledgePointId, UUID parentKnowledgePointId, int depth,
                                       UUID packageId, UUID packageVersionId) {
        return new DiagnosticProbe(UUID.randomUUID(), sessionId, knowledgePointId, parentKnowledgePointId, depth,
                packageId, packageVersionId, packageId == null ? Status.NO_SET : Status.OPEN, null, null, null, null,
                null);
    }
}
