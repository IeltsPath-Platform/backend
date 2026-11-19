package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.Min;

public record AddPackageVersionRequest(
        @Min(value = 1, message = "Số phiên bản phải từ 1")
        int versionNumber,

        String rulesJson
) {}

