package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import com.ieltspath.user.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.ieltspath.user.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {
    private static final int MAX_ERROR_LENGTH = 500;
    private final OutboxEventJpaRepository jpa;
    private final OutboxTraceContext traceContext;

    @Override public OutboxEvent save(OutboxEvent event) {
        OutboxEventJpaEntity row = new OutboxEventJpaEntity();
        row.setId(event.id()); row.setAggregateType(event.aggregateType()); row.setAggregateId(event.aggregateId());
        row.setEventType(event.eventType()); row.setPayload(event.payload()); row.setStatus(event.status());
        row.setRetryCount(event.retryCount()); row.setCreatedAt(event.createdAt());
        row.setCorrelationId(Correlation.current()); row.setTraceContext(traceContext.capture());
        return toDomain(jpa.save(row));
    }

    @Override public List<OutboxEvent> claimPending(int limit, int maxAttempts) {
        return jpa.claimPending(limit, maxAttempts).stream().map(this::toDomain).toList();
    }

    @Override public void markPublished(UUID id, Instant at) { jpa.markPublished(id, at); }

    @Override public void recordFailure(UUID id, String errorType, int maxAttempts) {
        String safe = errorType == null ? "unknown" : errorType;
        jpa.recordFailure(id, safe.substring(0, Math.min(safe.length(), MAX_ERROR_LENGTH)), maxAttempts);
    }

    @Override public int clearExpiredFailedPayloads(Instant olderThan) { return jpa.clearExpiredFailedPayloads(olderThan); }

    private OutboxEvent toDomain(OutboxEventJpaEntity row) {
        return new OutboxEvent(row.getId(), row.getAggregateType(), row.getAggregateId(), row.getEventType(),
                row.getPayload(), row.getStatus(), row.getRetryCount(), row.getCreatedAt(), row.getPublishedAt(),
                row.getCorrelationId(), row.getTraceContext());
    }
}
