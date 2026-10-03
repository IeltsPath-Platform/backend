package com.group01.learning.application.result;

import java.util.List;

/**
 * Every answered set shows its solutions and audio transcript, passed or not. {@code stage} and {@code failedSets}
 * describe the review after this set; they are null in responses stored before they existed.
 */
public record ReviewSubmissionResult(String reviewStatus, boolean setPassed,
                                     List<SubmissionResult.AnswerResult> results, String transcript,
                                     String stage, Integer failedSets) {
    public ReviewSubmissionResult(String reviewStatus, boolean setPassed, List<SubmissionResult.AnswerResult> results,
                                  String transcript) {
        this(reviewStatus, setPassed, results, transcript, null, null);
    }
}
