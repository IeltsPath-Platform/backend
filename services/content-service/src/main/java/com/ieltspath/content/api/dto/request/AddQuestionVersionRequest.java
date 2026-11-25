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
        @Min(value = 1, message = "Số phiên bản phải từ 1")
        int versionNumber,

        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        String stem,

        List<QuestionOptionPayload> options,
        String answerSpecJson,
        String explanation,

        @Size(max = 500, message = "Gợi ý không được dài quá 500 ký tự")
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
