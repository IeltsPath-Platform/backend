package com.group01.content.api.dto.request;

import com.group01.content.domain.vo.AssetType;
import jakarta.validation.constraints.NotNull;

public record CreateContentAssetRequest(
        @NotNull(message = "assetType is required")
        AssetType assetType,

        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum
) {}

