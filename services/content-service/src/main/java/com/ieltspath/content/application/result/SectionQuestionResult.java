package com.ieltspath.content.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SectionQuestionResult(
        UUID id,
        UUID sectionId,
        UUID questionVersionId,
        int sortOrder,
        BigDecimal maxScore,
        Instant createdAt,
        UUID groupId
) {
    public SectionQuestionResult(UUID id, UUID sectionId, UUID questionVersionId, int sortOrder,
                                 BigDecimal maxScore, Instant createdAt) {
        this(id, sectionId, questionVersionId, sortOrder, maxScore, createdAt, null);
    }
}

