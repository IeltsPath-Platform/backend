package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A validated, normalized LLM grade. {@code overallBand} is computed by Learning Service from the criteria. */
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand, List<StudyPoint> studyPoints, String judgmentVersion) {
    public WritingGrade {
        studyPoints = studyPoints == null ? List.of() : List.copyOf(studyPoints);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand) {
        this(criteria, corrections, summary, overallBand, List.of(), null);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand,
                        List<StudyPoint> studyPoints) {
        this(criteria, corrections, summary, overallBand, studyPoints, null);
    }

    /** {@code knowledgePointId} is the point this criterion is evidence for; null for grades stored before the mapping. */
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                            UUID knowledgePointId, List<KnowledgeJudgment> knowledgePointJudgments) {
        public Criterion {
            knowledgePointJudgments = knowledgePointJudgments == null ? List.of() : List.copyOf(knowledgePointJudgments);
        }

        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                         UUID knowledgePointId) {
            this(code, band, strengths, improvements, knowledgePointId, List.of());
        }

        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {
            this(code, band, strengths, improvements, null, List.of());
        }

        public Criterion withKnowledgePointJudgments(List<KnowledgeJudgment> judgments) {
            return new Criterion(code, band, strengths, improvements, knowledgePointId, judgments);
        }
    }

    public enum Judgment { PASS, FAIL, NOT_ASSESSED }

    public record KnowledgeJudgment(UUID knowledgePointId, String code, String name, Judgment judgment,
                                    String excerpt, String reason) {
        public KnowledgeJudgment {
            if (knowledgePointId == null || judgment == null) {
                throw new IllegalArgumentException("knowledgePointId and judgment are required");
            }
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
                             String name, String title, String reason, String excerpt, UUID practiceKnowledgePointId) {
        public static final String CRITERION = "CRITERION";
        public static final String AI = "AI";
        public static final String KNOWLEDGE_POINT = "KNOWLEDGE_POINT";

        public StudyPoint(String kind, String criterionCode, BigDecimal band, UUID knowledgePointId, String code,
                          String name, String title, String reason) {
            this(kind, criterionCode, band, knowledgePointId, code, name, title, reason, null, null);
        }
    }
}
