package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One judged answer as the MP-1.0 formula sees it. {@code questionVersionId} identifies the question for the
 * same-question rule and is null for evidence that does not come from a single question.
 */
public record EvidencePoint(boolean correct, EvidenceSource source, Instant occurredAt, UUID questionVersionId) {
    public EvidencePoint {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
