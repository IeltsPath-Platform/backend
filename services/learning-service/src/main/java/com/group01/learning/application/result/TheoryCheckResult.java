package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

/** A quick check after the theory always shows the answers and returns the review to {@code PRACTICE}. */
public record TheoryCheckResult(UUID reviewId, int correct, int total, List<SubmissionResult.AnswerResult> results,
                                String stage) {}
