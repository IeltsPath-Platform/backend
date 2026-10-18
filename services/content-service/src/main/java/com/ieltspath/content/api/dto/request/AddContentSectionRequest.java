package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AddContentSectionRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions,
        @Min(1) @Max(4) Integer partNo,
        UUID coverAssetId
) {}

