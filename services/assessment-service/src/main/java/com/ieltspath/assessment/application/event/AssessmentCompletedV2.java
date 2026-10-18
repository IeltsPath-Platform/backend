package com.ieltspath.assessment.application.event;

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

    /**
     * {@code learningGoalId} is always null: goals no longer scope learning. {@code packageVersionId} is the Content
     * version the attempt was created from. {@code overallBand} is the grader's band, or null when none was recorded.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Data(UUID userId, UUID learningGoalId, UUID packageVersionId, UUID attemptId, UUID resultId,
                       int resultVersion, String assessmentType, String status, Instant completedAt, BigDecimal overallBand,
                       List<ItemResult> itemResults) {}

    /**
     * {@code isCorrect} is null when the item was not graded as right/wrong (for example an essay). {@code durationMs}
     * is the learner's time on the item, or null when none was reported. {@code errorCode} is the error type of a wrong
     * answer that has one, or null. Both were added after v2 shipped and are optional for consumers.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemResult(UUID itemResultId, UUID questionVersionId, Boolean isCorrect, BigDecimal score,
                             BigDecimal maxScore, List<KnowledgePointMapping> knowledgePointMappings,
                             Long durationMs, String errorCode) {

        public ItemResult(UUID itemResultId, UUID questionVersionId, Boolean isCorrect, BigDecimal score,
                          BigDecimal maxScore, List<KnowledgePointMapping> knowledgePointMappings) {
            this(itemResultId, questionVersionId, isCorrect, score, maxScore, knowledgePointMappings, null, null);
        }
    }

    /** Weight is attribution metadata only. Judgment and error type are present only when a grader recorded them. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight, String qualitativeJudgment,
                                        String errorType) {}
}
