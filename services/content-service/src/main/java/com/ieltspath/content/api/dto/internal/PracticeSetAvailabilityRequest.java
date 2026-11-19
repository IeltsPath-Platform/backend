package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Omitted fields take defaults: no exclusions, at least 3 questions. */
public record PracticeSetAvailabilityRequest(
        @NotEmpty(message = "Cần chọn ít nhất một điểm kiến thức")
        @Size(max = 50, message = "Chỉ chọn tối đa 50 điểm kiến thức")
        List<@NotNull UUID> knowledgePointIds,
        @Size(max = 1000, message = "Danh sách gói loại trừ tối đa 1000 gói")
        List<@NotNull UUID> excludePackageIds,
        @Min(value = 1, message = "Số câu tối thiểu phải từ 1")
        Integer minQuestions
) {}
