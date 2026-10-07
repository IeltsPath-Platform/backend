package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublishPackageRequest(
        @NotNull(message = "versionId is required")
        UUID versionId
) {}

