package com.group01.learning.api.dto.request;

import com.group01.learning.api.dto.request.SubmitExerciseRequest.Answer;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.command.SubmitReviewCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitReviewRequest(@NotNull UUID reviewSetId, @NotNull UUID requestId,
                                  @NotNull List<@NotNull @Valid Answer> answers) {
    public SubmitReviewCommand toCommand() {
        return new SubmitReviewCommand(reviewSetId, requestId, answers.stream()
                .map(answer -> new SubmitExerciseCommand.Answer(answer.questionVersionId(), answer.answer())).toList());
    }
}
