package com.group01.content.application.result;

import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionVersionResult(
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
        List<QuestionKnowledgePoint> knowledgePoints
) {}

