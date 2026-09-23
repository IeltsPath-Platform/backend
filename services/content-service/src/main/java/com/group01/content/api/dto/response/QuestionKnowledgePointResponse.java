package com.group01.content.api.dto.response;

import com.group01.content.domain.entity.QuestionKnowledgePoint;

import java.math.BigDecimal;
import java.util.UUID;

public record QuestionKnowledgePointResponse(
        UUID questionVersionId,
        UUID knowledgePointId,
        BigDecimal weight
) {
    public static QuestionKnowledgePointResponse from(QuestionKnowledgePoint entity) {
        return new QuestionKnowledgePointResponse(
                entity.getQuestionVersionId(),
                entity.getKnowledgePointId(),
                entity.getWeight()
        );
    }
}

