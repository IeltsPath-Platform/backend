package com.group01.content.application.result;

import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record KnowledgePointResult(
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
    public static KnowledgePointResult from(KnowledgePoint kp) {
        return new KnowledgePointResult(
                kp.getId(),
                kp.getTopicId(),
                kp.getCode(),
                kp.getName(),
                kp.getKind(),
                kp.getLearningType(),
                kp.getSkill(),
                kp.getDescription(),
                kp.getStatus(),
                kp.getCreatedAt(),
                kp.getUpdatedAt()
        );
    }
}
