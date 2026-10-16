package com.ieltspath.learning.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A completed result version from {@code AssessmentCompleted.v2}, already checked against the contract. */
public record AssessmentResult(UUID eventId, UUID userId, UUID packageVersionId, UUID attemptId, UUID resultId,
                               int resultVersion, String assessmentType, Instant completedAt,
                               List<ItemResult> items, BigDecimal overallBand) {
    public AssessmentResult(UUID eventId, UUID userId, UUID packageVersionId, UUID attemptId, UUID resultId,
                            int resultVersion, String assessmentType, Instant completedAt, List<ItemResult> items) {
        this(eventId, userId, packageVersionId, attemptId, resultId, resultVersion, assessmentType, completedAt,
                items, null);
    }
    public AssessmentResult {
        items = List.copyOf(items);
    }

    /** {@code isCorrect} is null when the item was not graded right or wrong (for example an essay). */
    public record ItemResult(UUID itemResultId, UUID questionVersionId, Boolean isCorrect, BigDecimal score,
                             BigDecimal maxScore, List<KnowledgePointJudgment> knowledgePoints,
                             Long durationMs, String errorCode) {
        public ItemResult {
            knowledgePoints = List.copyOf(knowledgePoints);
        }

        /** The v2 shape before {@code duration_ms} and {@code error_code} were added. */
        public ItemResult(UUID itemResultId, UUID questionVersionId, Boolean isCorrect, BigDecimal score,
                          BigDecimal maxScore, List<KnowledgePointJudgment> knowledgePoints) {
            this(itemResultId, questionVersionId, isCorrect, score, maxScore, knowledgePoints, null, null);
        }

        /** Explicit correctness wins; otherwise a PASS or FAIL judgment of the KP; otherwise no evidence. */
        public Boolean correctnessFor(KnowledgePointJudgment judgment) {
            if (isCorrect != null) return isCorrect;
            if ("PASS".equals(judgment.qualitativeJudgment())) return true;
            if ("FAIL".equals(judgment.qualitativeJudgment())) return false;
            return null;
        }
    }

    public record KnowledgePointJudgment(UUID knowledgePointId, String qualitativeJudgment) {}
}
