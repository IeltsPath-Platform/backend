package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.entity.ContentAssetLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentAssetRepository {
    ContentAsset save(ContentAsset asset);
    Optional<ContentAsset> findById(UUID id);
    ContentAssetLink saveLink(ContentAssetLink link);
    List<ContentAsset> findBySectionId(UUID sectionId);
    List<ContentAsset> findByQuestionVersionId(UUID questionVersionId);
}

