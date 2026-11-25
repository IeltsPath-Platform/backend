package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkAssetRequest(
        @NotNull(message = "Cần chọn tài nguyên")
        UUID assetId,

        UUID sectionId,
        UUID questionVersionId,
        int sortOrder
) {}

