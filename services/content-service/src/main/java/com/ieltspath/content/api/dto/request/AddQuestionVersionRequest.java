package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddQuestionVersionRequest(
        @Min(value = 1, message = "versionNumber must be at least 1")
        int versionNumber,

        @NotBlank(message = "stem is required")
        String stem,

        List<QuestionOptionPayload> options,
        String answerSpecJson,
        String explanation,

        @Size(max = 500, message = "hint must be at most 500 characters")
        String hint,

        QuestionDifficulty difficulty,
        List<KnowledgePointRequest> knowledgePoints
) {
    public record KnowledgePointRequest(
            UUID questionVersionId,
            UUID knowledgePointId,
            BigDecimal weight
    ) {
    }
}
