package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Omitted fields take defaults: no exclusions, at least 3 questions, one result, no preferred lesson. */
public record PracticeSetSearchRequest(
        @NotNull(message = "Cần chọn điểm kiến thức")
        UUID knowledgePointId,
        @Size(max = 1000, message = "Danh sách gói loại trừ tối đa 1000 gói")
        List<@NotNull UUID> excludePackageIds,
        @Min(value = 1, message = "Số câu tối thiểu phải từ 1")
        Integer minQuestions,
        @Min(value = 1, message = "Số lượng phải từ 1 đến 10")
        @Max(value = 10, message = "Số lượng phải từ 1 đến 10")
        Integer limit,
        UUID preferredLessonId,
        String trapErrorCode
) {}
