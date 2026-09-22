package com.group01.content.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ContentAssetLink {
    private final UUID id;
    private final UUID assetId;
    private final UUID sectionId;
    private final UUID questionVersionId;
    private int sortOrder;
    private final Instant createdAt;

    public ContentAssetLink(UUID id, UUID assetId, UUID sectionId, UUID questionVersionId,
                            int sortOrder, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        if ((sectionId == null && questionVersionId == null) || (sectionId != null && questionVersionId != null)) {
            throw new IllegalArgumentException("Exactly one of sectionId or questionVersionId must be non-null");
        }
        this.sectionId = sectionId;
        this.questionVersionId = questionVersionId;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ContentAssetLink forSection(UUID assetId, UUID sectionId, int sortOrder) {
        return new ContentAssetLink(UUID.randomUUID(), assetId, sectionId, null, sortOrder, Instant.now());
    }

    public static ContentAssetLink forQuestionVersion(UUID assetId, UUID questionVersionId, int sortOrder) {
        return new ContentAssetLink(UUID.randomUUID(), assetId, null, questionVersionId, sortOrder, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public UUID getSectionId() { return sectionId; }
    public UUID getQuestionVersionId() { return questionVersionId; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}

