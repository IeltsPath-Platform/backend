package com.group01.content.api.dto.response;

import java.math.BigDecimal;
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
        Instant updatedAt,
        // The point's own range; null ends are open.
        BigDecimal bandMin,
        BigDecimal bandMax,
        // The range that applies: the point's own range if it has one, otherwise its topic's.
        BigDecimal effectiveBandMin,
        BigDecimal effectiveBandMax
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
                result.updatedAt(),
                result.band().min(),
                result.band().max(),
                result.effectiveBand().min(),
                result.effectiveBand().max()
        );
    }
}

