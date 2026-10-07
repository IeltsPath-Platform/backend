package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateContentPackageRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must not exceed 100 characters")
        String code,

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        @NotNull(message = "packageType is required")
        PackageType packageType,

        AccessLevel accessLevel,
        // Only for PRACTICE_SET: the lesson whose Practice the set belongs to.
        UUID lessonId
) {}
