package com.group01.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/** A validated, normalized LLM grade. {@code overallBand} is computed by Learning Service from the criteria. */
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand) {
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {}

    /** {@code excerpt} is an exact substring of the essay. */
    public record Correction(String excerpt, String suggestion, String category) {}
}
