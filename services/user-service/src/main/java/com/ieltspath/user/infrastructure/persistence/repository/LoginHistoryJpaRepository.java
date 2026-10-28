package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.LoginHistoryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginHistoryJpaRepository extends JpaRepository<LoginHistoryJpaEntity, UUID> {
    Page<LoginHistoryJpaEntity> findByUserId(UUID userId, Pageable pageable);
}
