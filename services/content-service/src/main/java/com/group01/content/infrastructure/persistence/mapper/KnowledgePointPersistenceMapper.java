package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.infrastructure.persistence.entity.KnowledgePointJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class KnowledgePointPersistenceMapper {

    public KnowledgePoint toDomain(KnowledgePointJpaEntity entity) {
        if (entity == null) return null;
        return new KnowledgePoint(
                entity.getId(),
                entity.getTopicId(),
                entity.getCode(),
                entity.getName(),
                entity.getKind(),
                entity.getSkill(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public KnowledgePointJpaEntity toEntity(KnowledgePoint domain) {
        if (domain == null) return null;
        return KnowledgePointJpaEntity.builder()
                .id(domain.getId())
                .topicId(domain.getTopicId())
                .code(domain.getCode())
                .name(domain.getName())
                .kind(domain.getKind())
                .skill(domain.getSkill())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

