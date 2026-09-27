package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.vo.BandRange;
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
                entity.getLearningType(),
                entity.getSkill(),
                entity.getDescription(),
                entity.getStatus(),
                BandRange.of(entity.getBandMin(), entity.getBandMax()),
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
                .learningType(domain.getLearningType())
                .skill(domain.getSkill())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .bandMin(domain.getBand().min())
                .bandMax(domain.getBand().max())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

