package com.ieltspath.learning.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * An examiner's finished standalone Writing review as Assessment reports it: each criterion's band, and each error code with
 * how often the examiner marked it. It carries no essay text, comment or examiner identity.
 */
public record HumanReviewResult(UUID eventId, UUID learnerId, UUID reviewSubmissionId, UUID sourceSubmissionId,
                                List<Criterion> criteria, List<ErrorCount> errorCodes) {
    public HumanReviewResult {
        criteria = List.copyOf(criteria);
        errorCodes = List.copyOf(errorCodes);
    }

    /** One criterion as the examiner named it ({@code taskResponse}, {@code coherence}, {@code lexical}, {@code grammar}). */
    public record Criterion(String criterion, BigDecimal band) { }

    /** An error code and how often the examiner marked it; at least 1. */
    public record ErrorCount(String code, int count) { }
}
