package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublishPackageRequest(
        @NotNull(message = "Cần chọn phiên bản")
        UUID versionId
) {}

