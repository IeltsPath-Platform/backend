package com.ieltspath.learning.api.dto.request;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record SubmitExerciseRequest(@NotNull UUID requestId, @NotNull List<@NotNull @Valid Answer> answers) {
    /** {@code durationMs} is the time the learner spent on this question; optional. Clients that do not measure it omit it. */
    public record Answer(@NotNull UUID questionVersionId, Object answer, @PositiveOrZero Long durationMs) {}

    public SubmitExerciseCommand toCommand() {
        return new SubmitExerciseCommand(requestId, answers.stream()
                .map(answer -> new SubmitExerciseCommand.Answer(answer.questionVersionId(), answer.answer(),
                        answer.durationMs())).toList());
    }
}
