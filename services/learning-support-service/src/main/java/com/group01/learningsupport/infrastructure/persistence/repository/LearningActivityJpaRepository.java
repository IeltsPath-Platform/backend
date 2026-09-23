package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.infrastructure.persistence.entity.LearningActivityJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningActivityJpaRepository extends JpaRepository<LearningActivityJpaEntity, UUID> {
    Page<LearningActivityJpaEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<LearningActivityJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
