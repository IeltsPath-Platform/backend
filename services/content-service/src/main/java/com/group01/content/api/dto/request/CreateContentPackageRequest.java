package com.group01.content.api.dto.request;

import com.group01.content.api.dto.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContentPackageRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must not exceed 100 characters")
        String code,

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        @NotNull(message = "packageType is required")
        PackageType packageType,

        AccessLevel accessLevel
) {}
