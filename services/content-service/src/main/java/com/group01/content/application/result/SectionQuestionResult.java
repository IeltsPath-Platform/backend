package com.group01.content.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SectionQuestionResult(
        UUID id,
        UUID sectionId,
        UUID questionVersionId,
        int sortOrder,
        BigDecimal maxScore,
        Instant createdAt
) {}

