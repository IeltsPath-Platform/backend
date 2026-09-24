package com.group01.content.api.dto.response;

import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record KnowledgePointResponse(
        UUID id,
        UUID topicId,
        String code,
        String name,
        KnowledgePointKind kind,
        LearningType learningType,
        Skill skill,
        String description,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgePointResponse from(KnowledgePointResult result) {
        return new KnowledgePointResponse(
                result.id(),
                result.topicId(),
                result.code(),
                result.name(),
                result.kind(),
                result.learningType(),
                result.skill(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

