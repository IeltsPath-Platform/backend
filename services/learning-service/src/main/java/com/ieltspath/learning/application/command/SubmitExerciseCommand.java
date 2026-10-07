package com.ieltspath.learning.application.command;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SubmitExerciseCommand(UUID requestId, List<Answer> answers) {
    public SubmitExerciseCommand {
        Objects.requireNonNull(requestId, "requestId is required");
        answers = List.copyOf(Objects.requireNonNull(answers, "answers are required"));
        for (Answer answer : answers) {
            Objects.requireNonNull(answer.questionVersionId(), "questionVersionId is required");
            if (answer.answer() != null && !(answer.answer() instanceof String)) {
                throw new IllegalArgumentException("An answer must be a string or null");
            }
        }
    }

    public record Answer(UUID questionVersionId, Object answer) {}
}
