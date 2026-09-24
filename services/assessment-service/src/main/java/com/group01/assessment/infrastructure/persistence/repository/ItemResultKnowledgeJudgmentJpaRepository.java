package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.ItemResultKnowledgeJudgmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItemResultKnowledgeJudgmentJpaRepository
        extends JpaRepository<ItemResultKnowledgeJudgmentJpaEntity, ItemResultKnowledgeJudgmentJpaEntity.Key> {

    @Query("select judgment from ItemResultKnowledgeJudgmentJpaEntity judgment "
            + "where judgment.id.itemResultId in :itemResultIds "
            + "order by judgment.id.itemResultId, judgment.id.knowledgePointId")
    List<ItemResultKnowledgeJudgmentJpaEntity> findByItemResultIds(
            @Param("itemResultIds") Collection<UUID> itemResultIds);
}
