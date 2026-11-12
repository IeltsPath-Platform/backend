package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A validated, normalized LLM grade. {@code overallBand} is computed by Learning Service from the criteria. */
public record WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary,
                           BigDecimal overallBand, List<StudyPoint> studyPoints, String judgmentVersion, String model,
                           List<AppliedBandLimit> bandLimits, List<Priority> priorities,
                           List<ErrorExample> errorExamples) {
    public WritingGrade {
        studyPoints = studyPoints == null ? List.of() : List.copyOf(studyPoints);
        bandLimits = bandLimits == null ? List.of() : List.copyOf(bandLimits);
        priorities = priorities == null ? List.of() : List.copyOf(priorities);
        errorExamples = errorExamples == null ? List.of() : List.copyOf(errorExamples);
    }

    /** Grades stored before priorities and error examples existed read with both empty. */
    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand,
                        List<StudyPoint> studyPoints, String judgmentVersion, String model,
                        List<AppliedBandLimit> bandLimits) {
        this(criteria, corrections, summary, overallBand, studyPoints, judgmentVersion, model, bandLimits, List.of(),
                List.of());
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand,
                        List<StudyPoint> studyPoints, String judgmentVersion, String model) {
        this(criteria, corrections, summary, overallBand, studyPoints, judgmentVersion, model, List.of());
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand) {
        this(criteria, corrections, summary, overallBand, List.of(), null, null);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand,
                        List<StudyPoint> studyPoints) {
        this(criteria, corrections, summary, overallBand, studyPoints, null, null);
    }

    public WritingGrade(List<Criterion> criteria, List<Correction> corrections, String summary, BigDecimal overallBand,
                        List<StudyPoint> studyPoints, String judgmentVersion) {
        this(criteria, corrections, summary, overallBand, studyPoints, judgmentVersion, null);
    }

    public WritingGrade withModel(String model) {
        return new WritingGrade(criteria, corrections, summary, overallBand, studyPoints, judgmentVersion, model,
                bandLimits, priorities, errorExamples);
    }

    /** The same grade with the details only the second prompt template returns. */
    public WritingGrade withDetails(List<Priority> priorities, List<ErrorExample> errorExamples) {
        return new WritingGrade(criteria, corrections, summary, overallBand, studyPoints, judgmentVersion, model,
                bandLimits, priorities, errorExamples);
    }

    /** A problem that costs the most band, chosen by the grader; the quotations are exact parts of the essay. */
    public record Priority(String title, String explanation, List<String> excerpts, String action,
                           List<String> errorCodes) {
        public Priority {
            excerpts = excerpts == null ? List.of() : List.copyOf(excerpts);
            errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
        }
    }

    /** A new short sentence, not taken from the essay, that shows the same error as {@code errorCode}. */
    public record ErrorExample(String errorCode, String wrong, String right) {}

    /**
     * A band cap that lowered a criterion. {@code bandBeforeLimit} is the model's band, kept so the cap can be audited
     * and reversed; the criterion band is the capped one.
     */
    public record AppliedBandLimit(String reason, String criterion, BigDecimal cap, BigDecimal bandBeforeLimit,
                                   String message) {}

    /** {@code knowledgePointId} is the point this criterion is evidence for; null for grades stored before the mapping. */
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                            UUID knowledgePointId, List<KnowledgeJudgment> knowledgePointJudgments,
                            List<String> evidence, String descriptor) {
        public Criterion {
            knowledgePointJudgments = knowledgePointJudgments == null ? List.of() : List.copyOf(knowledgePointJudgments);
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }

        /** {@code evidence} and {@code descriptor} are only returned by the second prompt template. */
        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                         UUID knowledgePointId, List<KnowledgeJudgment> knowledgePointJudgments) {
            this(code, band, strengths, improvements, knowledgePointId, knowledgePointJudgments, List.of(), null);
        }

        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                         UUID knowledgePointId) {
            this(code, band, strengths, improvements, knowledgePointId, List.of());
        }

        public Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {
            this(code, band, strengths, improvements, null, List.of());
        }

        public Criterion withKnowledgePointJudgments(List<KnowledgeJudgment> judgments) {
            return new Criterion(code, band, strengths, improvements, knowledgePointId, judgments, evidence, descriptor);
        }

        public Criterion withBand(BigDecimal newBand) {
            return new Criterion(code, newBand, strengths, improvements, knowledgePointId, knowledgePointJudgments,
                    evidence, descriptor);
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
    /** {@code practiceKnowledgePointCode} is set with {@code practiceKnowledgePointId}; null for grades stored before it. */
    public record Correction(String excerpt, String suggestion, String category, String errorCode, String errorName,
                             String fixHint, Integer severity, UUID practiceKnowledgePointId,
                             String practiceKnowledgePointCode, String reason, String certainty) {
        public static final String ERROR = "ERROR";
        public static final String SUGGESTION = "SUGGESTION";

        /** {@code reason} and {@code certainty} are only returned by the second prompt template; null reads as ERROR. */
        public Correction(String excerpt, String suggestion, String category, String errorCode, String errorName,
                          String fixHint, Integer severity, UUID practiceKnowledgePointId,
                          String practiceKnowledgePointCode) {
            this(excerpt, suggestion, category, errorCode, errorName, fixHint, severity, practiceKnowledgePointId,
                    practiceKnowledgePointCode, null, null);
        }

        public Correction(String excerpt, String suggestion, String category, String errorCode, String errorName,
                          String fixHint, Integer severity, UUID practiceKnowledgePointId) {
            this(excerpt, suggestion, category, errorCode, errorName, fixHint, severity, practiceKnowledgePointId, null);
        }

        public Correction(String excerpt, String suggestion, String category) {
            this(excerpt, suggestion, category, null, null, null, null, null, null);
        }

        public Correction(String excerpt, String suggestion, String category, String errorCode) {
            this(excerpt, suggestion, category, errorCode, null, null, null, null, null);
        }

        /** A suggestion is acceptable text with a better wording; it is not counted as an error. */
        public boolean isSuggestion() { return SUGGESTION.equals(certainty); }
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
