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
    @Query("delete from LoginHistoryJpaEntity history where history.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
