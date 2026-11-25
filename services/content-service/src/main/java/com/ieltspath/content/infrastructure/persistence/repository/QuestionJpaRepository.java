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
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

    Optional<QuestionJpaEntity> findFirstByImportRef(String importRef);

    boolean existsByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"versions"})
    Optional<QuestionJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"versions"})
    List<QuestionJpaEntity> findAllByIdIn(List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"versions"})
    @Query("select distinct q from QuestionJpaEntity q where q.id in "
            + "(select owner.id from QuestionJpaEntity owner join owner.versions version where version.id in :versionIds) "
            + "order by q.id")
    List<QuestionJpaEntity> findAllByVersionIdsForUpdate(@Param("versionIds") List<UUID> versionIds);

    @Query(value = "SELECT q FROM QuestionJpaEntity q WHERE (:skill IS NULL OR q.skill = :skill)"
            + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
            + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))"
            + " ORDER BY q.createdAt DESC, q.id",
            countQuery = "SELECT COUNT(q) FROM QuestionJpaEntity q WHERE (:skill IS NULL OR q.skill = :skill)"
                    + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
                    + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<QuestionJpaEntity> findPage(@Param("skill") Skill skill, @Param("purpose") QuestionPurpose purpose,
                                     @Param("status") PublicationStatus status, @Param("code") String code, Pageable pageable);

    @Query(value = "SELECT q.* FROM questions q WHERE (:skill IS NULL OR q.skill = :skill)"
            + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
            + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))"
            + " AND NOT EXISTS (SELECT 1 FROM lesson_block_questions lbq"
            + " WHERE lbq.question_version_id = q.current_published_version_id)"
            + " AND NOT EXISTS (SELECT 1 FROM section_questions sq"
            + " WHERE sq.question_version_id = q.current_published_version_id)"
            + " ORDER BY q.created_at DESC, q.id",
            countQuery = "SELECT COUNT(*) FROM questions q WHERE (:skill IS NULL OR q.skill = :skill)"
                    + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
                    + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))"
                    + " AND NOT EXISTS (SELECT 1 FROM lesson_block_questions lbq"
                    + " WHERE lbq.question_version_id = q.current_published_version_id)"
                    + " AND NOT EXISTS (SELECT 1 FROM section_questions sq"
                    + " WHERE sq.question_version_id = q.current_published_version_id)", nativeQuery = true)
    Page<QuestionJpaEntity> findFreePage(@Param("skill") String skill, @Param("purpose") String purpose,
                                         @Param("status") String status, @Param("code") String code,
                                         Pageable pageable);

    @Query(value = "SELECT q.* FROM questions q WHERE (:skill IS NULL OR q.skill = :skill)"
            + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
            + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))"
            + " AND NOT EXISTS (SELECT 1 FROM lesson_block_questions lbq"
            + " WHERE lbq.question_version_id = q.current_published_version_id AND lbq.block_id <> :exceptBlockId)"
            + " AND NOT EXISTS (SELECT 1 FROM section_questions sq"
            + " WHERE sq.question_version_id = q.current_published_version_id)"
            + " ORDER BY q.created_at DESC, q.id",
            countQuery = "SELECT COUNT(*) FROM questions q WHERE (:skill IS NULL OR q.skill = :skill)"
                    + " AND (:purpose IS NULL OR q.purpose = :purpose) AND (:status IS NULL OR q.status = :status)"
                    + " AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))"
                    + " AND NOT EXISTS (SELECT 1 FROM lesson_block_questions lbq"
                    + " WHERE lbq.question_version_id = q.current_published_version_id AND lbq.block_id <> :exceptBlockId)"
                    + " AND NOT EXISTS (SELECT 1 FROM section_questions sq"
                    + " WHERE sq.question_version_id = q.current_published_version_id)", nativeQuery = true)
    Page<QuestionJpaEntity> findFreePageExceptBlock(@Param("skill") String skill, @Param("purpose") String purpose,
                                                    @Param("status") String status, @Param("code") String code,
                                                    @Param("exceptBlockId") java.util.UUID exceptBlockId,
                                                    Pageable pageable);
}

