package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.TheoryCheckResult;

import java.util.List;
import java.util.UUID;

/** Quick-check answers are always shown; the review is back at {@code PRACTICE}. */
public record TheoryCheckResponse(UUID reviewId, int correct, int total, List<SubmissionResponse.SolvedAnswer> results,
                                  String stage) {
    public static TheoryCheckResponse from(TheoryCheckResult result) {
        return new TheoryCheckResponse(result.reviewId(), result.correct(), result.total(), result.results().stream()
                .map(answer -> new SubmissionResponse.SolvedAnswer(answer.questionVersionId(), answer.correct(),
                        answer.correctAnswer(), answer.explanation())).toList(), result.stage());
    }
}
