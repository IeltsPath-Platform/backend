package com.group01.content.api.dto.request;

import java.math.BigDecimal;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
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

        int sortOrder,
        @DecimalMin(value = "0.0", message = "bandMin must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMin must be at most 9.0")
        BigDecimal bandMin,
        @DecimalMin(value = "0.0", message = "bandMax must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMax must be at most 9.0")
        BigDecimal bandMax,
        // The skill the topic's lessons teach; optional, never ALL.
        Skill skill,
        UUID courseId
) {
    public CreateTopicRequest(UUID parentTopicId, String code, String name, int sortOrder,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill) {
        this(parentTopicId, code, name, sortOrder, bandMin, bandMax, skill, null);
    }
}

