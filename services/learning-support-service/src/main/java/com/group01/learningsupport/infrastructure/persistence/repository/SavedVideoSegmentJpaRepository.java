package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.infrastructure.persistence.entity.SavedVideoSegmentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SavedVideoSegmentJpaRepository extends JpaRepository<SavedVideoSegmentJpaEntity, UUID> {
    Page<SavedVideoSegmentJpaEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<SavedVideoSegmentJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
