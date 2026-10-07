package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.infrastructure.persistence.entity.ContentAssetLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentAssetLinkJpaRepository extends JpaRepository<ContentAssetLinkJpaEntity, UUID> {
    List<ContentAssetLinkJpaEntity> findBySectionIdOrderBySortOrderAsc(UUID sectionId);
    List<ContentAssetLinkJpaEntity> findByQuestionVersionIdOrderBySortOrderAsc(UUID questionVersionId);
}

