package com.group01.assessment.domain.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of one Content question-version to knowledge-point mapping taken when the attempt started.
 * The weight is attribution metadata only; it never scales scores or mastery.
 */
public record AttemptItemKnowledgePoint(UUID attemptItemId, UUID knowledgePointId, BigDecimal weight) {
    public AttemptItemKnowledgePoint {
        Objects.requireNonNull(attemptItemId, "attemptItemId must not be null");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        if (weight.signum() < 0) {
            throw new IllegalArgumentException("Knowledge-point weight must not be negative");
        }
    }
}
