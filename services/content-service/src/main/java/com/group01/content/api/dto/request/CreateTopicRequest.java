package com.group01.content.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTopicRequest(
        UUID parentTopicId,

        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must not exceed 100 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        int sortOrder
) {}

