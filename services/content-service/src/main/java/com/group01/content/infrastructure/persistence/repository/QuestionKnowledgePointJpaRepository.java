package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.QuestionKnowledgePointId;
import com.group01.content.infrastructure.persistence.entity.QuestionKnowledgePointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionKnowledgePointJpaRepository
        extends JpaRepository<QuestionKnowledgePointJpaEntity, QuestionKnowledgePointId> {

    @Query("select mapping from QuestionKnowledgePointJpaEntity mapping "
            + "where mapping.id.questionVersionId in :questionVersionIds "
            + "order by mapping.id.questionVersionId, mapping.id.knowledgePointId")
    List<QuestionKnowledgePointJpaEntity> findByQuestionVersionIds(
            @Param("questionVersionIds") Collection<UUID> questionVersionIds);
}
