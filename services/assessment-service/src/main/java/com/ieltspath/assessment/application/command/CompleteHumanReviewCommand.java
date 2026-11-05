package com.ieltspath.assessment.application.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * An Examiner's finished independent review: the band, the band of each criterion, error codes, the errors marked in
 * the essay and written feedback.
 */
public record CompleteHumanReviewCommand(BigDecimal band, List<Criterion> criteria, List<String> errorCodes,
                                         List<Annotation> annotations, String feedback) {
    public record Criterion(String criterion, BigDecimal band, String comment) {}

    /** One error marked in the essay. {@code start} and {@code end} are UTF-16 offsets, {@code end} exclusive. */
    public record Annotation(String excerpt, int start, int end, String errorCode, String suggestion) {}
}
