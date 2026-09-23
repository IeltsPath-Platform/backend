package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.Topic;
import com.group01.content.infrastructure.persistence.entity.TopicJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TopicPersistenceMapper {

    public Topic toDomain(TopicJpaEntity entity) {
        if (entity == null) return null;
        return new Topic(
                entity.getId(),
                entity.getParentTopicId(),
                entity.getCode(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TopicJpaEntity toEntity(Topic domain) {
        if (domain == null) return null;
        return TopicJpaEntity.builder()
                .id(domain.getId())
                .parentTopicId(domain.getParentTopicId())
                .code(domain.getCode())
                .name(domain.getName())
                .sortOrder(domain.getSortOrder())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

