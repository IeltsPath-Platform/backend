package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.LoginHistoryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LoginHistoryJpaRepository extends JpaRepository<LoginHistoryJpaEntity, UUID> {
    Page<LoginHistoryJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @Modifying
    @Query(value = """
            DELETE FROM login_history
            WHERE id IN (
                SELECT id FROM login_history
                WHERE created_at < :cutoff
                ORDER BY created_at, id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    @Query("select count(history) from LoginHistoryJpaEntity history where history.createdAt < :cutoff")
    long countOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
