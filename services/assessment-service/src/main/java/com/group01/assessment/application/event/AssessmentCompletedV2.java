package com.group01.assessment.application.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Self-contained announcement that one formal result version is final. Consumers must not call back into
 * Assessment or Content to interpret it: correctness, scores and the knowledge-point snapshot travel with it.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AssessmentCompletedV2(UUID eventId, String eventType, Instant occurredAt, String source, Data data) {
    public static final String EVENT_TYPE = "AssessmentCompleted.v2";
    public static final String SOURCE = "assessment-service";

    /** {@code overallBand} is the grader's band for this version, or null when none was recorded. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Data(UUID userId, UUID learningGoalId, UUID attemptId, UUID resultId, int resultVersion,
                       String assessmentType, String status, Instant completedAt, BigDecimal overallBand,
                       List<ItemResult> itemResults) {}

    /** {@code isCorrect} is null when the item was not graded as right/wrong (for example an essay). */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemResult(UUID itemResultId, UUID questionVersionId, Boolean isCorrect, BigDecimal score,
                             BigDecimal maxScore, List<KnowledgePointMapping> knowledgePointMappings) {}

    /** Weight is attribution metadata only. Judgment and error type are present only when a grader recorded them. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight, String qualitativeJudgment,
                                        String errorType) {}
}
