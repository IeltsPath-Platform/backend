package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.AuthAuditEventJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuthAuditEventJpaRepository extends JpaRepository<AuthAuditEventJpaEntity, UUID> {
    Page<AuthAuditEventJpaEntity> findByUserId(UUID userId, Pageable pageable);

    Page<AuthAuditEventJpaEntity> findByUserIdAndEventType(UUID userId, String eventType, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM auth_audit_events WHERE id IN (SELECT id FROM auth_audit_events " +
            "WHERE created_at < :cutoff ORDER BY created_at, id LIMIT :batchSize FOR UPDATE SKIP LOCKED)", nativeQuery = true)
    int deleteExpiredBatch(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
