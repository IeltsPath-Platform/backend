package com.group01.user.infrastructure.persistence.repository;

import com.group01.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearnerProfileJpaRepository extends JpaRepository<LearnerProfileJpaEntity, UUID> {
    Optional<LearnerProfileJpaEntity> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}

