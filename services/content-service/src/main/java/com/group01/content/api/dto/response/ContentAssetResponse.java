package com.group01.content.api.dto.response;

import com.group01.content.application.result.ContentAssetResult;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.AssetValidationStatus;

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
        Instant createdAt
) {
    public static ContentAssetResponse from(ContentAssetResult result) {
        return new ContentAssetResponse(
                result.id(),
                result.assetType(),
                result.textContent(),
                result.mediaReference(),
                result.durationSeconds(),
                result.checksum(),
                result.validationStatus(),
                result.createdAt()
        );
    }
}

