package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.infrastructure.persistence.entity.LearningVideoJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningVideoJpaRepository extends JpaRepository<LearningVideoJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"segments", "segments.lexicalEntries"})
    Optional<LearningVideoJpaEntity> findByYoutubeVideoId(String youtubeVideoId);

    @Override
    @EntityGraph(attributePaths = {"segments", "segments.lexicalEntries"})
    Optional<LearningVideoJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"segments", "segments.lexicalEntries"})
    Optional<LearningVideoJpaEntity> findDistinctBySegments_Id(UUID segmentId);

    @Query("SELECT v FROM LearningVideoJpaEntity v WHERE (:featureRequired IS NULL OR (:featureRequired = true AND v.requiredFeatureKey IS NOT NULL) OR (:featureRequired = false AND v.requiredFeatureKey IS NULL)) AND (:status IS NULL OR v.status = :status) ORDER BY v.createdAt DESC")
    List<LearningVideoJpaEntity> findAllFiltered(@Param("featureRequired") Boolean featureRequired,
                                                 @Param("status") PublicationStatus status);

    boolean existsByYoutubeVideoId(String youtubeVideoId);
}
