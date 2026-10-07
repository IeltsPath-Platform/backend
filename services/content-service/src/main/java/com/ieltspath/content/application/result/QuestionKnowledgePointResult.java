package com.ieltspath.content.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record QuestionKnowledgePointResult(
        UUID questionVersionId,
        UUID knowledgePointId,
        BigDecimal weight
) {
}
