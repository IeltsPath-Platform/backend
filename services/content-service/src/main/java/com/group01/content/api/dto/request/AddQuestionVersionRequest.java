package com.group01.content.api.dto.request;

import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AddQuestionVersionRequest(
        @Min(value = 1, message = "versionNumber must be at least 1")
        int versionNumber,

        @NotBlank(message = "stem is required")
        String stem,

        List<QuestionOptionPayload> options,
        String answerSpecJson,
        String explanation,
        QuestionDifficulty difficulty,
        List<QuestionKnowledgePoint> knowledgePoints
) {}

