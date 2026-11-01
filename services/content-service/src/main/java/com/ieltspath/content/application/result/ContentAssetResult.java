package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;
import com.ieltspath.content.domain.vo.AccessTier;

import java.time.Instant;
import java.util.UUID;

public record ContentAssetResult(
        UUID id,
        AssetType assetType,
        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum,
        AssetValidationStatus validationStatus,
        Instant createdAt,
        AccessTier accessTier
) {
    public ContentAssetResult(UUID id, AssetType assetType, String textContent, String mediaReference,
                              Integer durationSeconds, String checksum, AssetValidationStatus validationStatus,
                              Instant createdAt) {
        this(id, assetType, textContent, mediaReference, durationSeconds, checksum, validationStatus, createdAt,
                AccessTier.PUBLIC);
    }
}

