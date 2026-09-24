package com.group01.content.api.dto.internal;

import com.group01.content.application.result.QuestionKnowledgePointResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record KnowledgePointMappingResponse(List<Mapping> mappings) {
    public static KnowledgePointMappingResponse from(List<QuestionKnowledgePointResult> results) {
        return new KnowledgePointMappingResponse(results.stream()
                .map(result -> new Mapping(result.questionVersionId(), result.knowledgePointId(), result.weight()))
                .toList());
    }

    public record Mapping(UUID questionVersionId, UUID knowledgePointId, BigDecimal weight) {}
}
