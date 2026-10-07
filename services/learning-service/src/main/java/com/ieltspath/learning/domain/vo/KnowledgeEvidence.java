package com.ieltspath.learning.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * One judged answer for a knowledge point. {@code sourceReferenceId} makes the row idempotent per source; formal
 * results also carry the attempt and result version so a regrade can replace them.
 */
public record KnowledgeEvidence(UUID knowledgePointId, boolean correct, EvidenceSource source, UUID sourceReferenceId,
                                UUID attemptId, Integer resultVersion) {
    public KnowledgeEvidence {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceReferenceId, "sourceReferenceId");
        if ((source == EvidenceSource.ASSESSMENT) != (attemptId != null && resultVersion != null)) {
            throw new IllegalArgumentException("Only assessment evidence carries an attempt and result version");
        }
    }

    public static KnowledgeEvidence of(UUID knowledgePointId, boolean correct, EvidenceSource source,
                                       UUID sourceReferenceId) {
        return new KnowledgeEvidence(knowledgePointId, correct, source, sourceReferenceId, null, null);
    }

    public static KnowledgeEvidence assessment(UUID knowledgePointId, boolean correct, UUID sourceReferenceId,
                                               UUID attemptId, int resultVersion) {
        return new KnowledgeEvidence(knowledgePointId, correct, EvidenceSource.ASSESSMENT, sourceReferenceId,
                attemptId, resultVersion);
    }
}
