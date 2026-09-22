package com.group01.content.api.dto.response;

import com.group01.content.application.result.QuestionVersionResult;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionVersionResponse(
        UUID id,
        UUID questionId,
        int versionNumber,
        String stem,
        List<QuestionOptionPayload> options,
        String answerSpecJson,
        String explanation,
        QuestionDifficulty difficulty,
        PublicationStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionKnowledgePointResponse> knowledgePoints
) {
    public static QuestionVersionResponse from(QuestionVersionResult result) {
        List<QuestionKnowledgePointResponse> kpResponses = result.knowledgePoints() != null
                ? result.knowledgePoints().stream().map(QuestionKnowledgePointResponse::from).toList()
                : List.of();
        return new QuestionVersionResponse(
                result.id(),
                result.questionId(),
                result.versionNumber(),
                result.stem(),
                result.options(),
                result.answerSpecJson(),
                result.explanation(),
                result.difficulty(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                kpResponses
        );
    }
}

