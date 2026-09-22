package com.group01.content.api.dto.request;

import jakarta.validation.constraints.Min;

public record AddPackageVersionRequest(
        @Min(value = 1, message = "versionNumber must be at least 1")
        int versionNumber,

        String rulesJson
) {}

