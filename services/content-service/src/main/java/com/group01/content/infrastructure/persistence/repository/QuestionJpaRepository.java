package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.domain.vo.Skill;
import com.group01.content.infrastructure.persistence.entity.QuestionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<QuestionJpaEntity> findBySkill(Skill skill);
}

