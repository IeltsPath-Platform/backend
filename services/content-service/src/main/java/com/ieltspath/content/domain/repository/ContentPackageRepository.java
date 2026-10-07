package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPackageRepository {
    ContentPackage save(ContentPackage pkg);
    Optional<ContentPackage> findById(UUID id);

    Optional<ContentPackage> findByVersionId(UUID versionId);

    Optional<ContentPackage> findBySectionId(UUID sectionId);
    Optional<ContentPackage> findByCode(String code);

    List<ContentPackage> findAll(Boolean featureRequired, PublicationStatus status);
    boolean existsByCode(String code);
}
