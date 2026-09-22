package com.group01.content.application.result;

import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.AssetValidationStatus;

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

