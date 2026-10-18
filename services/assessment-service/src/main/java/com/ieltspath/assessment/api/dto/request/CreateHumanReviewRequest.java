package com.ieltspath.assessment.api.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateHumanReviewRequest(
        @NotNull UUID requestId,
        /** Optional and never trusted: the review always belongs to the authenticated learner; a different id is refused. */
        UUID learnerId,
        @NotNull UUID sourceSubmissionId,
        @NotNull Source source,
        @NotBlank @Pattern(regexp = "TASK_[12]") String task,
        @NotNull JsonNode promptSnapshot,
        @NotBlank String essay,
        @NotNull JsonNode rubric
) {
    public enum Source { LESSON, PRACTICE }
}
