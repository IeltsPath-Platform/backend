package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.domain.vo.WritingGrade;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Learner view of one essay. A GRADED essay carries the grade; PAYMENT_PENDING carries only {@code code}; GRADING and
 * FAILED carry {@code failureCode}. Chart facts and the answer spec are never part of it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WritingSubmissionResponse(UUID submissionId, String status, String task, Integer wordCount,
                                        BigDecimal overallBand, Boolean passed, List<Criterion> criteria,
                                        List<Correction> corrections, List<StudyPoint> studyPoints, String summary, Integer pointsCharged,
                                        String sampleAnswer, String code, String failureCode, String essayText,
                                        List<BandLimit> bandLimits, DataChecks dataChecks, TaskChecks taskChecks) {
    /** {@code limited} is true only when a band cap lowered this criterion; absent otherwise. */
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements,
                            List<KnowledgeJudgment> knowledgePointJudgments, Boolean limited) {}

    /**
     * Task 1 only. The sentences the essay got wrong about the chart, with what the chart shows, and how many were right.
     * A flag the model did not give is left out. The chart facts themselves never leave the service.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DataChecks(List<WrongFigure> wrong, int correctCount, Boolean hasOverview, Boolean overviewAccurate,
                             Boolean hasPersonalOpinion) {}

    public record WrongFigure(String excerpt, String expected) {}

    /** Task 2 only: which parts of the question the essay answered, its position, its outline, and the flags drawn from them. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskChecks(List<QuestionPart> questionParts, Position position, List<OutlineItem> outline,
                             boolean partialAnswer, boolean noPosition, boolean inconsistentPosition,
                             boolean noConclusion) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionPart(String part, String addressed, String excerpt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Position(boolean required, boolean stated, String excerpt, Boolean consistent, String conflictExcerpt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OutlineItem(String role, String mainIdea, boolean developed, String excerpt) {}

    /** Same shape as Speaking's {@code bandLimits}, plus {@code criterion}: the cap names the criterion it lowers. */
    public record BandLimit(String reason, String criterion, BigDecimal cap, String message) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KnowledgeJudgment(UUID knowledgePointId, String code, String name, String judgment, String excerpt,
                                    String reason) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Correction(String excerpt, String suggestion, String category, String errorCode, String errorName,
                             String fixHint, Integer severity, UUID practiceKnowledgePointId,
                             String practiceKnowledgePointCode) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StudyPoint(String kind, String criterionCode, BigDecimal band, UUID knowledgePointId, String code,
                             String name, String title, String reason, String excerpt, UUID practiceKnowledgePointId) {}

    public static WritingSubmissionResponse from(WritingSubmissionResult result) {
        var grade = result.grade();
        return new WritingSubmissionResponse(result.submissionId(), result.status(),
                "GRADED".equals(result.status()) ? result.task() : null, result.wordCount(), result.overallBand(),
                result.passed(),
                grade == null ? null : grade.criteria().stream().map(criterion -> new Criterion(criterion.code(),
                        criterion.band(), criterion.strengths(), criterion.improvements(),
                        criterion.knowledgePointJudgments().stream().map(judgment -> new KnowledgeJudgment(
                                judgment.knowledgePointId(), judgment.code(), judgment.name(),
                                judgment.judgment().name(), judgment.excerpt(), judgment.reason())).toList(),
                        limited(grade, criterion.code()))).toList(),
                grade == null ? null : grade.corrections().stream().map(correction -> new Correction(
                        correction.excerpt(), correction.suggestion(), correction.category(), correction.errorCode(),
                        correction.errorName(), correction.fixHint(), correction.severity(),
                        correction.practiceKnowledgePointId(), correction.practiceKnowledgePointCode())).toList(),
                grade == null ? null : grade.studyPoints().stream().map(point -> new StudyPoint(point.kind(),
                        point.criterionCode(), point.band(), point.knowledgePointId(), point.code(), point.name(),
                        point.title(), point.reason(), point.excerpt(), point.practiceKnowledgePointId())).toList(),
                grade == null ? null : grade.summary(), result.pointsCharged(), result.sampleAnswer(), result.code(),
                result.failureCode(), result.essayText(), bandLimits(grade), dataChecks(grade), taskChecks(grade));
    }

    private static DataChecks dataChecks(WritingGrade grade) {
        if (grade == null || grade.task1Checks() == null) return null;
        var checks = grade.task1Checks();
        return new DataChecks(checks.dataChecks().stream()
                .filter(check -> WritingGrade.DataCheck.WRONG.equals(check.verdict()))
                .map(check -> new WrongFigure(check.excerpt(), check.expected())).toList(),
                checks.correctCount(), checks.hasOverview(), checks.overviewAccurate(), checks.hasPersonalOpinion());
    }

    private static TaskChecks taskChecks(WritingGrade grade) {
        if (grade == null || grade.task2Checks() == null) return null;
        var checks = grade.task2Checks();
        var position = checks.position();
        return new TaskChecks(checks.questionParts().stream()
                .map(part -> new QuestionPart(part.part(), part.addressed(), part.excerpt())).toList(),
                position == null ? null : new Position(position.required(), position.stated(), position.excerpt(),
                        position.consistent(), position.conflictExcerpt()),
                checks.outline().stream().map(item -> new OutlineItem(item.role(), item.mainIdea(), item.developed(),
                        item.excerpt())).toList(),
                checks.partialAnswer(), checks.noPosition(), checks.inconsistentPosition(), checks.noConclusion());
    }

    private static Boolean limited(WritingGrade grade, String criterion) {
        return grade != null && grade.bandLimits().stream().anyMatch(limit -> limit.criterion().equals(criterion))
                ? Boolean.TRUE : null;
    }

    private static List<BandLimit> bandLimits(WritingGrade grade) {
        if (grade == null || grade.bandLimits().isEmpty()) return null;
        return grade.bandLimits().stream().map(limit -> new BandLimit(limit.reason(), limit.criterion(), limit.cap(),
                limit.message())).toList();
    }
}
