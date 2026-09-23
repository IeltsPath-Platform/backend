package com.group01.content.api.dto.response;

import com.group01.content.application.result.SectionQuestionResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SectionQuestionResponse(
        UUID id,
        UUID sectionId,
        UUID questionVersionId,
        int sortOrder,
        BigDecimal maxScore,
        Instant createdAt
) {
    public static SectionQuestionResponse from(SectionQuestionResult result) {
        return new SectionQuestionResponse(
                result.id(),
                result.sectionId(),
                result.questionVersionId(),
                result.sortOrder(),
                result.maxScore(),
                result.createdAt()
        );
    }
}

