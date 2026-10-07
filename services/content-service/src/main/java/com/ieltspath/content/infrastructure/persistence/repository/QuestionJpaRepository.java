package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.infrastructure.persistence.entity.QuestionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"versions"})
    Optional<QuestionJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"versions"})
    List<QuestionJpaEntity> findAllByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"versions"})
    List<QuestionJpaEntity> findBySkill(Skill skill);

    @Query("SELECT q FROM QuestionJpaEntity q WHERE q.purpose = :purpose AND (:skill IS NULL OR q.skill = :skill)")
    List<QuestionJpaEntity> findByPurpose(@Param("purpose") QuestionPurpose purpose, @Param("skill") Skill skill);
}

