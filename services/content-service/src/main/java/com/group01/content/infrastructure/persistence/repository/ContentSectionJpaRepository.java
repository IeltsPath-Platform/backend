package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.ContentSectionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentSectionJpaRepository extends JpaRepository<ContentSectionJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"questions"})
    List<ContentSectionJpaEntity> findByPackageVersionIdOrderBySortOrderAsc(UUID packageVersionId);
}

