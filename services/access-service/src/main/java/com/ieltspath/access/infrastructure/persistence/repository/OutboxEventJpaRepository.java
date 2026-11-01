package com.ieltspath.access.infrastructure.persistence.repository;

import com.ieltspath.access.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    // SKIP LOCKED lets several relay instances drain the table without publishing the same row concurrently.
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING' AND retry_count < :maxAttempts
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> claimPending(@Param("limit") int limit, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("update OutboxEventJpaEntity event set event.status = 'PUBLISHED', event.publishedAt = :publishedAt, "
            + "event.lastError = null where event.id = :id")
    int markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("update OutboxEventJpaEntity event set event.retryCount = event.retryCount + 1, "
            + "event.lastError = :error, "
            + "event.status = case when event.retryCount + 1 >= :maxAttempts then 'FAILED' else event.status end "
            + "where event.id = :id")
    int recordFailure(@Param("id") UUID id, @Param("error") String error, @Param("maxAttempts") int maxAttempts);
}
