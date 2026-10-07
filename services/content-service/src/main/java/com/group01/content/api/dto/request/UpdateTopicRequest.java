package com.group01.content.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTopicRequest(
        UUID parentTopicId,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        int sortOrder,
        ContentStatus status,
        @DecimalMin(value = "0.0", message = "bandMin must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMin must be at most 9.0")
        BigDecimal bandMin,
        @DecimalMin(value = "0.0", message = "bandMax must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMax must be at most 9.0")
        BigDecimal bandMax,
        // Null keeps the current skill; a change is refused once the topic has published lessons.
        Skill skill,
        UUID courseId
) {
    public UpdateTopicRequest(UUID parentTopicId, String name, int sortOrder, ContentStatus status,
                              BigDecimal bandMin, BigDecimal bandMax, Skill skill) {
        this(parentTopicId, name, sortOrder, status, bandMin, bandMax, skill, null);
    }
}

