package com.group01.learning.api.dto;

import com.group01.learning.application.result.SubmissionResult;

import java.util.List;
import java.util.UUID;

public record SubmissionResponse(boolean blockPassed, boolean lessonCompleted, List<Result> results) {
    public sealed interface Result permits Correctness, SolvedAnswer {}
    public record Correctness(UUID questionVersionId, boolean correct) implements Result {}
    public record SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer,
                              String explanation) implements Result {}

    public static SubmissionResponse from(SubmissionResult result) {
        List<Result> answers = result.results().stream().<Result>map(answer -> result.blockPassed()
                ? new SolvedAnswer(answer.questionVersionId(), answer.correct(), answer.correctAnswer(), answer.explanation())
                : new Correctness(answer.questionVersionId(), answer.correct())).toList();
        return new SubmissionResponse(result.blockPassed(), result.lessonCompleted(), answers);
    }
}
