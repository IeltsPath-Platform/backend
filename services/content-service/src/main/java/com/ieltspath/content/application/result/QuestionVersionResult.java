package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;

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
        String hint,
        QuestionDifficulty difficulty,
        PublicationStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionKnowledgePointResult> knowledgePoints
) {}
