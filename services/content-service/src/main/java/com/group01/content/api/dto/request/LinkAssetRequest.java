package com.group01.content.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkAssetRequest(
        @NotNull(message = "assetId is required")
        UUID assetId,

        UUID sectionId,
        UUID questionVersionId,
        int sortOrder
) {}

