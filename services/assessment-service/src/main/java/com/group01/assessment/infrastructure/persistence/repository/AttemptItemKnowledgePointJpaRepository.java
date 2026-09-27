package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AttemptItemKnowledgePointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptItemKnowledgePointJpaRepository
        extends JpaRepository<AttemptItemKnowledgePointJpaEntity, AttemptItemKnowledgePointJpaEntity.Key> {

    @Query("select mapping from AttemptItemKnowledgePointJpaEntity mapping "
            + "where mapping.id.attemptItemId in :attemptItemIds "
            + "order by mapping.id.attemptItemId, mapping.id.knowledgePointId")
    List<AttemptItemKnowledgePointJpaEntity> findByAttemptItemIds(
            @Param("attemptItemIds") Collection<UUID> attemptItemIds);
}
