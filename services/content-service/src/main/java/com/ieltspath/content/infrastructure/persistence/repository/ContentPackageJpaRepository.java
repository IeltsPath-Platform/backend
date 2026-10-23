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

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findDistinctByVersions_Id(UUID versionId);

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findDistinctByVersions_Sections_Id(UUID sectionId);

    @Query("SELECT p FROM ContentPackageJpaEntity p WHERE (:featureRequired IS NULL OR (:featureRequired = true AND p.requiredFeatureKey IS NOT NULL) OR (:featureRequired = false AND p.requiredFeatureKey IS NULL)) AND (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<ContentPackageJpaEntity> findAllFiltered(@Param("featureRequired") Boolean featureRequired,
                                                  @Param("status") PublicationStatus status);

    boolean existsByCode(String code);
}
