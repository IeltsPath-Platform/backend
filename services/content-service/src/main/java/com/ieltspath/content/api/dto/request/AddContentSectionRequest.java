package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddContentSectionRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions
) {}

