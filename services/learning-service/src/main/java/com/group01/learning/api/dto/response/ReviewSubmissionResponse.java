package com.group01.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group01.learning.application.result.ReviewSubmissionResult;

import java.util.List;

/** Every answered set shows its solutions and the audio transcript, passed or not. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewSubmissionResponse(String reviewStatus, List<SubmissionResponse.Result> results,
                                       String transcript, String stage, Integer failedSets) {
    public static ReviewSubmissionResponse from(ReviewSubmissionResult result) {
        List<SubmissionResponse.Result> answers = result.results().stream().<SubmissionResponse.Result>map(answer ->
                new SubmissionResponse.SolvedAnswer(answer.questionVersionId(), answer.correct(),
                        answer.correctAnswer(), answer.explanation())).toList();
        return new ReviewSubmissionResponse(result.reviewStatus(), answers, result.transcript(), result.stage(),
                result.failedSets());
    }
}
