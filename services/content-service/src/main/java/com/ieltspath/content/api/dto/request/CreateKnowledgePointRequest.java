package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
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
        String description
) {}
