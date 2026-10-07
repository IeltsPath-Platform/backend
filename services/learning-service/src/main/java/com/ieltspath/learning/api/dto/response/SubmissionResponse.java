package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.SubmissionResult;

import java.util.List;
import java.util.UUID;

public record SubmissionResponse(boolean blockPassed, boolean lessonCompleted, List<Result> results) {
    public sealed interface Result permits Correctness, SolvedAnswer {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Correctness(UUID questionVersionId, boolean correct, String hint) implements Result {
        public Correctness(UUID questionVersionId, boolean correct) {
            this(questionVersionId, correct, null);
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer,
                              String explanation, String hint) implements Result {
        public SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {
            this(questionVersionId, correct, correctAnswer, explanation, null);
        }
    }

    public static SubmissionResponse from(SubmissionResult result) {
        List<Result> answers = result.results().stream().<Result>map(answer -> result.blockPassed()
                ? new SolvedAnswer(answer.questionVersionId(), answer.correct(), answer.correctAnswer(), answer.explanation())
                : new Correctness(answer.questionVersionId(), answer.correct(), answer.hint())).toList();
        return new SubmissionResponse(result.blockPassed(), result.lessonCompleted(), answers);
    }
}
