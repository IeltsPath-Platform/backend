package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.repository.AuthAuditEventRepository;
import com.ieltspath.user.domain.vo.OwnedPage;
import com.ieltspath.user.infrastructure.persistence.entity.AuthAuditEventJpaEntity;
import com.ieltspath.user.infrastructure.persistence.repository.AuthAuditEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuthAuditEventRepositoryAdapter implements AuthAuditEventRepository {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private final AuthAuditEventJpaRepository jpa;

    @Override
    public AuthAuditEvent save(AuthAuditEvent event) {
        var saved = jpa.save(toEntity(event));
        return toDomain(saved);
    }

    @Override
    public OwnedPage<AuthAuditEvent> findByUserId(UUID userId, String eventType, int page, int size) {
        Page<AuthAuditEventJpaEntity> result = eventType == null
                ? jpa.findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST))
                : jpa.findByUserIdAndEventType(userId, eventType, PageRequest.of(page, size, NEWEST_FIRST));
        return new OwnedPage<>(result.getContent().stream().map(AuthAuditEventRepositoryAdapter::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public int deleteExpiredBatch(LocalDateTime cutoff, int batchSize) {
        return jpa.deleteExpiredBatch(cutoff, batchSize);
    }

    private static AuthAuditEventJpaEntity toEntity(AuthAuditEvent event) {
        return AuthAuditEventJpaEntity.builder().id(event.id()).userId(event.userId()).eventType(event.eventType())
                .ipAddress(event.ipAddress()).userAgent(event.userAgent()).correlationId(event.correlationId())
                .detail(event.detail()).createdAt(event.createdAt()).build();
    }

    private static AuthAuditEvent toDomain(AuthAuditEventJpaEntity entity) {
        return new AuthAuditEvent(entity.getId(), entity.getUserId(), entity.getEventType(), entity.getIpAddress(),
                entity.getUserAgent(), entity.getCorrelationId(), entity.getDetail(), entity.getCreatedAt());
    }
}
