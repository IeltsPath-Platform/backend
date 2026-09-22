package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.QuestionVersionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionVersionJpaRepository extends JpaRepository<QuestionVersionJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"knowledgePoints"})
    Optional<QuestionVersionJpaEntity> findByQuestionIdAndVersionNumber(UUID questionId, int versionNumber);

    @EntityGraph(attributePaths = {"knowledgePoints"})
    List<QuestionVersionJpaEntity> findByQuestionIdOrderByVersionNumberAsc(UUID questionId);
}

