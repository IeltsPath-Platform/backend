package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.entity.ContentAssetLink;
import com.group01.content.infrastructure.persistence.entity.ContentAssetJpaEntity;
import com.group01.content.infrastructure.persistence.entity.ContentAssetLinkJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContentAssetPersistenceMapper {

    public ContentAsset toDomain(ContentAssetJpaEntity entity) {
        if (entity == null) return null;
        return new ContentAsset(
                entity.getId(),
                entity.getAssetType(),
                entity.getTextContent(),
                entity.getMediaReference(),
                entity.getDurationSeconds(),
                entity.getChecksum(),
                entity.getValidationStatus(),
                entity.getCreatedAt()
        );
    }

    public ContentAssetJpaEntity toEntity(ContentAsset domain) {
        if (domain == null) return null;
        return ContentAssetJpaEntity.builder()
                .id(domain.getId())
                .assetType(domain.getAssetType())
                .textContent(domain.getTextContent())
                .mediaReference(domain.getMediaReference())
                .durationSeconds(domain.getDurationSeconds())
                .checksum(domain.getChecksum())
                .validationStatus(domain.getValidationStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public ContentAssetLink toLinkDomain(ContentAssetLinkJpaEntity entity) {
        if (entity == null) return null;
        return new ContentAssetLink(
                entity.getId(),
                entity.getAssetId(),
                entity.getSectionId(),
                entity.getQuestionVersionId(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }

    public ContentAssetLinkJpaEntity toLinkEntity(ContentAssetLink domain) {
        if (domain == null) return null;
        return ContentAssetLinkJpaEntity.builder()
                .id(domain.getId())
                .assetId(domain.getAssetId())
                .sectionId(domain.getSectionId())
                .questionVersionId(domain.getQuestionVersionId())
                .sortOrder(domain.getSortOrder())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

