package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;
import com.ieltspath.content.domain.vo.AccessTier;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ContentAsset {
    private final UUID id;
    private AssetType assetType;
    private String textContent;
    private String mediaReference;
    private Integer durationSeconds;
    private String checksum;
    private AssetValidationStatus validationStatus;
    private AccessTier accessTier;
    private final Instant createdAt;

    public ContentAsset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds, String checksum, AssetValidationStatus validationStatus,
                        Instant createdAt) {
        this(id, assetType, textContent, mediaReference, durationSeconds, checksum, validationStatus, createdAt,
                AccessTier.PUBLIC);
    }

    public ContentAsset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds, String checksum, AssetValidationStatus validationStatus,
                        Instant createdAt, AccessTier accessTier) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.assetType = Objects.requireNonNull(assetType, "assetType must not be null");
        this.textContent = textContent;
        this.mediaReference = mediaReference;
        this.durationSeconds = durationSeconds;
        this.checksum = checksum;
        this.validationStatus = validationStatus != null ? validationStatus : AssetValidationStatus.VALID;
        this.accessTier = accessTier != null ? accessTier : AccessTier.PUBLIC;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ContentAsset create(AssetType assetType, String textContent, String mediaReference,
                                      Integer durationSeconds, String checksum) {
        return create(assetType, textContent, mediaReference, durationSeconds, checksum, AccessTier.PUBLIC);
    }

    public static ContentAsset create(AssetType assetType, String textContent, String mediaReference,
                                      Integer durationSeconds, String checksum, AccessTier accessTier) {
        return new ContentAsset(UUID.randomUUID(), assetType, textContent, mediaReference,
                durationSeconds, checksum, AssetValidationStatus.VALID, Instant.now(), accessTier);
    }

    public UUID getId() { return id; }
    public AssetType getAssetType() { return assetType; }
    public String getTextContent() { return textContent; }
    public String getMediaReference() { return mediaReference; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public String getChecksum() { return checksum; }
    public AssetValidationStatus getValidationStatus() { return validationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public AccessTier getAccessTier() { return accessTier; }
}

