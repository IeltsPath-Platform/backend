package com.group01.learning.application.result;

import java.util.List;

/** {@code transcript} is the set's audio transcript, revealed only when the set passed. */
public record ReviewSubmissionResult(String reviewStatus, boolean setPassed,
                                     List<SubmissionResult.AnswerResult> results, String transcript) {}
