package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.ContentPackageVersionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentPackageVersionJpaRepository extends JpaRepository<ContentPackageVersionJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"sections", "sections.questions"})
    Optional<ContentPackageVersionJpaEntity> findByPackageIdAndVersionNumber(UUID packageId, int versionNumber);

    @EntityGraph(attributePaths = {"sections", "sections.questions"})
    List<ContentPackageVersionJpaEntity> findByPackageIdOrderByVersionNumberAsc(UUID packageId);
}

