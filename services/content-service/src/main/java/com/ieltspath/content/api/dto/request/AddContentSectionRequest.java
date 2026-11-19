package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AddContentSectionRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề không được dài quá 255 ký tự")
        String title,

        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions,
        @Min(1) @Max(4) Integer partNo,
        UUID coverAssetId
) {}

