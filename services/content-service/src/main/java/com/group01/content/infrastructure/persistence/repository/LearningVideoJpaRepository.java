package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.domain.vo.AccessLevel;
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

    @Query("SELECT v FROM LearningVideoJpaEntity v WHERE (:accessLevel IS NULL OR v.accessLevel = :accessLevel) AND (:status IS NULL OR v.status = :status) ORDER BY v.createdAt DESC")
    List<LearningVideoJpaEntity> findAllFiltered(@Param("accessLevel") AccessLevel accessLevel,
                                                 @Param("status") PublicationStatus status);

    boolean existsByYoutubeVideoId(String youtubeVideoId);
}

