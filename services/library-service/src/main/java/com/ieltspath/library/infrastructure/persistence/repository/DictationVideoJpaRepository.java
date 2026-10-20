package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.domain.vo.PublicationStatus;
import com.ieltspath.library.domain.vo.VideoLevel;
import com.ieltspath.library.infrastructure.persistence.entity.LearningVideoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Video lookups for dictation that never fetch the segment collections. */
public interface DictationVideoJpaRepository extends JpaRepository<LearningVideoJpaEntity, UUID> {
    Optional<LearningVideoJpaEntity> findByIdAndStatus(UUID id, PublicationStatus status);

    List<LearningVideoJpaEntity> findByStatusOrderByLevelAscTitleAsc(PublicationStatus status);

    List<LearningVideoJpaEntity> findByStatusAndLevelOrderByTitleAsc(PublicationStatus status, VideoLevel level);
}
