package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.infrastructure.persistence.entity.VideoLearningProgressJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoLearningProgressJpaRepository extends JpaRepository<VideoLearningProgressJpaEntity, UUID> {
    Optional<VideoLearningProgressJpaEntity> findByUserIdAndVideoId(UUID userId, UUID videoId);

    Optional<VideoLearningProgressJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<VideoLearningProgressJpaEntity> findByUserId(UUID userId, Pageable pageable);
}
