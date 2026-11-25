package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.vo.OwnedPage;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuthAuditEventRepository {
    AuthAuditEvent save(AuthAuditEvent event);
    OwnedPage<AuthAuditEvent> findByUserId(UUID userId, String eventType, int page, int size);
    int deleteExpiredBatch(LocalDateTime cutoff, int batchSize);
}
