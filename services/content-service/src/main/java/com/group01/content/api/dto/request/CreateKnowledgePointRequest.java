package com.group01.content.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateKnowledgePointRequest(
        @NotNull(message = "topicId is required")
        UUID topicId,

        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must not exceed 100 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        @NotNull(message = "kind is required")
        KnowledgePointKind kind,

        @NotNull(message = "learningType is required")
        LearningType learningType,

        Skill skill,
        String description,
        @DecimalMin(value = "0.0", message = "bandMin must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMin must be at most 9.0")
        BigDecimal bandMin,
        @DecimalMin(value = "0.0", message = "bandMax must be at least 0.0")
        @DecimalMax(value = "9.0", message = "bandMax must be at most 9.0")
        BigDecimal bandMax
) {}

