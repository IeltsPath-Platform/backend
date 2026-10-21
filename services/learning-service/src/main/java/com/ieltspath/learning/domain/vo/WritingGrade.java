package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A validated, normalized LLM grade. {@code overallBand} is computed by Learning Service from the criteria. */
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand) {
    /** {@code knowledgePointId} is the point this criterion is evidence for; null for grades stored before the mapping. */
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                            UUID knowledgePointId) {
        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {
            this(code, band, strengths, improvements, null);
        }
    }

    /** {@code excerpt} is an exact substring of the essay. */
    public record Correction(String excerpt, String suggestion, String category, String errorCode) {
        public Correction(String excerpt, String suggestion, String category) {
            this(excerpt, suggestion, category, null);
        }
    }
}
