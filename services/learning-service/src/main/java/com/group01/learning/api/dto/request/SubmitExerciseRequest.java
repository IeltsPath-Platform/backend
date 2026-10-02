package com.group01.learning.api.dto;

import com.group01.learning.application.command.SubmitExerciseCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitExerciseRequest(@NotNull UUID requestId, @NotNull List<@NotNull @Valid Answer> answers) {
    public record Answer(@NotNull UUID questionVersionId, Object answer) {}

    public SubmitExerciseCommand toCommand() {
        return new SubmitExerciseCommand(requestId, answers.stream()
                .map(answer -> new SubmitExerciseCommand.Answer(answer.questionVersionId(), answer.answer())).toList());
    }
}
