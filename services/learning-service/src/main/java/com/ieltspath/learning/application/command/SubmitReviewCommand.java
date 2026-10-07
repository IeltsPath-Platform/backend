package com.ieltspath.learning.application.command;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SubmitReviewCommand(UUID reviewSetId, UUID requestId, List<SubmitExerciseCommand.Answer> answers) {
    public SubmitReviewCommand {
        Objects.requireNonNull(reviewSetId, "reviewSetId is required");
        // Reuse the exercise command's answer checks.
        answers = new SubmitExerciseCommand(requestId, answers).answers();
    }
}
