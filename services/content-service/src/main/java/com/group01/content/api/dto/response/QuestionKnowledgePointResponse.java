package com.group01.content.api.dto.response;

import com.group01.content.application.result.QuestionKnowledgePointResult;

import java.math.BigDecimal;
import java.util.UUID;

public record QuestionKnowledgePointResponse(
        UUID questionVersionId,
        UUID knowledgePointId,
        BigDecimal weight
) {
    public static QuestionKnowledgePointResponse from(QuestionKnowledgePointResult result) {
        return new QuestionKnowledgePointResponse(
                result.questionVersionId(),
                result.knowledgePointId(),
                result.weight()
        );
    }
}
