package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.ContentAssetResult;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;
import com.ieltspath.content.domain.vo.AccessTier;

import java.time.Instant;
import java.util.UUID;

public record ContentAssetResponse(
        UUID id,
        AssetType assetType,
        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum,
        AssetValidationStatus validationStatus,
        Instant createdAt,
        AccessTier accessTier,
        /** A URL the author can open to see or hear the asset; null for text assets and when it cannot be resolved. */
        String mediaUrl
) {
    public static ContentAssetResponse from(ContentAssetResult result) {
        return from(result, null);
    }

    public static ContentAssetResponse from(ContentAssetResult result, String mediaUrl) {
        return new ContentAssetResponse(
                result.id(),
                result.assetType(),
                result.textContent(),
                result.mediaReference(),
                result.durationSeconds(),
                result.checksum(),
                result.validationStatus(),
                result.createdAt(),
                result.accessTier(),
                mediaUrl
        );
    }
}

