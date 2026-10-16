package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.infrastructure.persistence.entity.KnowledgePointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgePointJpaRepository extends JpaRepository<KnowledgePointJpaEntity, UUID> {
    Optional<KnowledgePointJpaEntity> findByCode(String code);
    List<KnowledgePointJpaEntity> findByTopicId(UUID topicId);
    boolean existsByCode(String code);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM question_knowledge_points qkp
                JOIN question_versions qv ON qv.id = qkp.question_version_id
                WHERE qkp.knowledge_point_id = :id AND qv.status = 'PUBLISHED'
            ) OR EXISTS (
                SELECT 1 FROM lesson_knowledge_points lkp WHERE lkp.knowledge_point_id = :id
            )
            """, nativeQuery = true)
    boolean isInUse(@Param("id") UUID id);
}

