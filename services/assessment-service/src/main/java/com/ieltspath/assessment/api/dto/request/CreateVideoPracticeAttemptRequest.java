package com.ieltspath.assessment.api.dto.request;

import com.ieltspath.assessment.domain.vo.PracticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVideoPracticeAttemptRequest(
        @NotNull UUID videoId,
        @NotNull UUID segmentId,
        @NotNull PracticeType practiceType,
        @NotBlank String referenceTextSnapshot) {
}
