package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;

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
        Instant createdAt
) {}

