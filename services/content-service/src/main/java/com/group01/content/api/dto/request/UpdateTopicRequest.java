package com.group01.content.api.dto.request;

import com.group01.content.domain.vo.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTopicRequest(
        UUID parentTopicId,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        int sortOrder,
        ContentStatus status
) {}

