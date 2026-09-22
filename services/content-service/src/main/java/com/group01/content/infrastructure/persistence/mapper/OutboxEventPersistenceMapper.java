package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.OutboxEvent;
import com.group01.content.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPersistenceMapper {

    public OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        if (entity == null) return null;
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getRetryCount(),
                entity.getLastError()
        );
    }

    public OutboxEventJpaEntity toEntity(OutboxEvent domain) {
        if (domain == null) return null;
        return OutboxEventJpaEntity.builder()
                .id(domain.getId())
                .aggregateType(domain.getAggregateType())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .createdAt(domain.getCreatedAt())
                .publishedAt(domain.getPublishedAt())
                .retryCount(domain.getRetryCount())
                .lastError(domain.getLastError())
                .build();
    }
}

