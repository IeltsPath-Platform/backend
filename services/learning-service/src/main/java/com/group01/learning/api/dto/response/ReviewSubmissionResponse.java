package com.group01.learning.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group01.learning.application.result.ReviewSubmissionResult;

import java.util.List;

/** Solutions and the audio transcript appear only for a passed set. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewSubmissionResponse(String reviewStatus, List<SubmissionResponse.Result> results,
                                       String transcript) {
    public static ReviewSubmissionResponse from(ReviewSubmissionResult result) {
        List<SubmissionResponse.Result> answers = result.results().stream().<SubmissionResponse.Result>map(answer ->
                result.setPassed()
                        ? new SubmissionResponse.SolvedAnswer(answer.questionVersionId(), answer.correct(),
                        answer.correctAnswer(), answer.explanation())
                        : new SubmissionResponse.Correctness(answer.questionVersionId(), answer.correct())).toList();
        return new ReviewSubmissionResponse(result.reviewStatus(), answers, result.transcript());
    }
}
