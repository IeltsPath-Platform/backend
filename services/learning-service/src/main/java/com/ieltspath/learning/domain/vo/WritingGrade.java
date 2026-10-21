package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A validated, normalized LLM grade. {@code overallBand} is computed by Learning Service from the criteria. */
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand, List<StudyPoint> studyPoints) {
    public WritingGrade {
        studyPoints = studyPoints == null ? List.of() : List.copyOf(studyPoints);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand) {
        this(criteria, corrections, summary, overallBand, List.of());
    }

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

    /** A weak rubric criterion and its knowledge point, or a focus suggested by the grader's model. */
    public record StudyPoint(String kind, String criterionCode, BigDecimal band, UUID knowledgePointId, String code,
                             String name, String title, String reason) {
        public static final String CRITERION = "CRITERION";
        public static final String AI = "AI";
    }
}
