package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.KnowledgePointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgePointJpaRepository extends JpaRepository<KnowledgePointJpaEntity, UUID> {
    Optional<KnowledgePointJpaEntity> findByCode(String code);
    List<KnowledgePointJpaEntity> findByTopicId(UUID topicId);
    boolean existsByCode(String code);
}

