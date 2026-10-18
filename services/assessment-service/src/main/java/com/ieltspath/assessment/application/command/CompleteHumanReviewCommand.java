package com.ieltspath.assessment.application.command;

import java.math.BigDecimal;
import java.util.List;

/** An Examiner's finished independent review: the band, the band of each criterion, error codes and written feedback. */
public record CompleteHumanReviewCommand(BigDecimal band, List<Criterion> criteria, List<String> errorCodes,
                                         String feedback) {
    public record Criterion(String criterion, BigDecimal band, String comment) {}
}
