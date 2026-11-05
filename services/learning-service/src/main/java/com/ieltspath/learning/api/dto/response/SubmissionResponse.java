package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.result.ErrorTrapResult;

import com.ieltspath.learning.domain.vo.GamificationSummary;

import java.util.List;
import java.util.UUID;

public record SubmissionResponse(boolean blockPassed, boolean lessonCompleted, List<Result> results,
                                 GamificationSummary gamification) {
    public sealed interface Result permits Correctness, SolvedAnswer {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Correctness(UUID questionVersionId, boolean correct, String hint,
                              @JsonInclude(JsonInclude.Include.NON_NULL) ErrorTrapResult error) implements Result {
        public Correctness(UUID questionVersionId, boolean correct) {
            this(questionVersionId, correct, null, null);
        }
        public Correctness(UUID questionVersionId, boolean correct, String hint) {
            this(questionVersionId, correct, hint, null);
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer,
                              String explanation, String hint,
                              @JsonInclude(JsonInclude.Include.NON_NULL) ErrorTrapResult error) implements Result {
        public SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {
            this(questionVersionId, correct, correctAnswer, explanation, null, null);
        }
        public SolvedAnswer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation,
                            String hint) {
            this(questionVersionId, correct, correctAnswer, explanation, hint, null);
        }
    }

    public static SubmissionResponse from(SubmissionResult result) {
        List<Result> answers = result.results().stream().<Result>map(answer -> result.blockPassed()
                ? new SolvedAnswer(answer.questionVersionId(), answer.correct(), answer.correctAnswer(),
                        answer.explanation(), answer.hint(), answer.error())
                : new Correctness(answer.questionVersionId(), answer.correct(), answer.hint(), answer.error())).toList();
        return new SubmissionResponse(result.blockPassed(), result.lessonCompleted(), answers, result.gamification());
    }
}
