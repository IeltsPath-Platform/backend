package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.infrastructure.persistence.entity.ContentPackageJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentPackageJpaRepository extends JpaRepository<ContentPackageJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"versions"})
    @Query("SELECT p FROM ContentPackageJpaEntity p WHERE p.id = :id")
    Optional<ContentPackageJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query(value = "SET CONSTRAINTS uq_content_sections_sort_order DEFERRED", nativeQuery = true)
    int deferSectionOrderCheck();

    @Modifying
    @Query(value = "SET CONSTRAINTS uq_section_questions_sort_order DEFERRED", nativeQuery = true)
    int deferQuestionOrderCheck();

    /**
     * Copies the sections of one version into another, with their question groups, questions and the passage and
     * audio linked to them. The copies get new ids; questions, assets and covers are shared, not duplicated. It is one
     * statement, so the foreign keys between the copied rows are checked when all of them exist.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            WITH section_map AS (
                SELECT id AS old_id, gen_random_uuid() AS new_id
                FROM content_sections WHERE package_version_id = :fromVersionId
            ), group_map AS (
                SELECT g.id AS old_id, gen_random_uuid() AS new_id
                FROM section_question_groups g JOIN section_map m ON m.old_id = g.section_id
            ), copied_sections AS (
                INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds,
                                              instructions, part_no, cover_asset_id)
                SELECT m.new_id, :toVersionId, s.title, s.skill, s.sort_order, s.time_limit_seconds,
                       s.instructions, s.part_no, s.cover_asset_id
                FROM content_sections s JOIN section_map m ON m.old_id = s.id
                RETURNING id
            ), copied_groups AS (
                INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions)
                SELECT gm.new_id, sm.new_id, g.sort_order, g.format_code, g.instructions
                FROM section_question_groups g
                JOIN group_map gm ON gm.old_id = g.id
                JOIN section_map sm ON sm.old_id = g.section_id
                RETURNING id
            ), copied_questions AS (
                INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id)
                SELECT gen_random_uuid(), sm.new_id, q.question_version_id, q.sort_order, q.max_score, gm.new_id
                FROM section_questions q
                JOIN section_map sm ON sm.old_id = q.section_id
                LEFT JOIN group_map gm ON gm.old_id = q.group_id
                RETURNING id
            )
            INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
            SELECT gen_random_uuid(), l.asset_id, sm.new_id, l.sort_order
            FROM content_asset_links l JOIN section_map sm ON sm.old_id = l.section_id
            """, nativeQuery = true)
    int copyVersionContent(@Param("fromVersionId") UUID fromVersionId, @Param("toVersionId") UUID toVersionId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM question_versions WHERE id = :id)", nativeQuery = true)
    boolean questionVersionExists(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE content_packages SET lesson_id = :lessonId, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = :packageId AND package_type = 'PRACTICE_SET' AND lesson_id IS NULL", nativeQuery = true)
    int attachPracticeSetToLesson(@Param("packageId") UUID packageId, @Param("lessonId") UUID lessonId);

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findDistinctByVersions_Id(UUID versionId);

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findDistinctByVersions_Sections_Id(UUID sectionId);

    @Query("SELECT p FROM ContentPackageJpaEntity p WHERE (:featureRequired IS NULL OR (:featureRequired = true AND p.requiredFeatureKey IS NOT NULL) OR (:featureRequired = false AND p.requiredFeatureKey IS NULL)) AND (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<ContentPackageJpaEntity> findAllFiltered(@Param("featureRequired") Boolean featureRequired,
                                                  @Param("status") PublicationStatus status);

    boolean existsByCode(String code);
}
