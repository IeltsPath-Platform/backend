package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.infrastructure.persistence.entity.QuestionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<QuestionJpaEntity> findFirstByImportRef(String importRef);

    @Override
    @EntityGraph(attributePaths = {"versions"})
    Optional<QuestionJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"versions"})
    List<QuestionJpaEntity> findAllByIdIn(List<UUID> ids);

    @Query(value = "SELECT q FROM QuestionJpaEntity q WHERE (:skill IS NULL OR q.skill = :skill)"
            + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
            + " ORDER BY q.createdAt DESC, q.id",
            countQuery = "SELECT COUNT(q) FROM QuestionJpaEntity q WHERE (:skill IS NULL OR q.skill = :skill)"
                    + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)")
    Page<QuestionJpaEntity> findPage(@Param("skill") Skill skill, @Param("purpose") QuestionPurpose purpose,
                                     @Param("status") PublicationStatus status, Pageable pageable);
}

