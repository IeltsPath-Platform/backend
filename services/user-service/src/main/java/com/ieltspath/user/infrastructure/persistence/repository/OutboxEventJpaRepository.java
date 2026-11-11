package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' AND retry_count < :maxAttempts " +
            "ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventJpaEntity> claimPending(@Param("limit") int limit, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("update OutboxEventJpaEntity event set event.status = 'PUBLISHED', event.publishedAt = :at, " +
            "event.lastError = null, event.payload = null where event.id = :id")
    int markPublished(@Param("id") UUID id, @Param("at") Instant at);

    @Modifying
    @Query("update OutboxEventJpaEntity event set event.retryCount = event.retryCount + 1, " +
            "event.lastError = :error, event.status = case when event.retryCount + 1 >= :maxAttempts " +
            "then 'FAILED' else event.status end where event.id = :id")
    int recordFailure(@Param("id") UUID id, @Param("error") String error, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query(value = "UPDATE outbox_events SET payload = NULL, status = 'FAILED' " +
            "WHERE payload IS NOT NULL AND status <> 'PUBLISHED' AND (" +
            "(event_type = 'user.password-reset-requested.v1' AND created_at < :resetBefore) OR " +
            "(event_type = 'user.email-verification-requested.v1' AND created_at < :verificationBefore))",
            nativeQuery = true)
    int expireLinkPayloads(@Param("resetBefore") Instant resetBefore, @Param("verificationBefore") Instant verificationBefore);
}
