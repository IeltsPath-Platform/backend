package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.infrastructure.persistence.entity.ContentPackageJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @EntityGraph(attributePaths = {"versions"})
    Optional<ContentPackageJpaEntity> findDistinctByVersions_Id(UUID versionId);

    @Query("SELECT p FROM ContentPackageJpaEntity p WHERE (:featureRequired IS NULL OR (:featureRequired = true AND p.requiredFeatureKey IS NOT NULL) OR (:featureRequired = false AND p.requiredFeatureKey IS NULL)) AND (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<ContentPackageJpaEntity> findAllFiltered(@Param("featureRequired") Boolean featureRequired,
                                                  @Param("status") PublicationStatus status);

    boolean existsByCode(String code);
}
