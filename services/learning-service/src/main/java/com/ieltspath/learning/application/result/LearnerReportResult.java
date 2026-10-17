package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The learner report: for each knowledge point the learner has evidence on, a fixed Score / Errors / Evidence / Action
 * block. {@code code}, {@code name}, {@code skill} and {@code band} come from Content and are null when it does not
 * list the point.
 */
public record LearnerReportResult(UUID userId, Instant generatedAt, List<KnowledgePointReport> knowledgePoints) {
    public record KnowledgePointReport(UUID knowledgeId, String code, String name, String skill, BigDecimal band,
                                       Score score, List<ErrorEntry> errors, List<EvidenceEntry> evidence,
                                       Action action) {}

    public record Score(double mastery, String tier, int evidenceCount) {}

    public record ErrorEntry(String code, int count) {}

    public record EvidenceEntry(UUID questionVersionId, Boolean correct, Long durationMs, String source,
                                String errorCode, Instant at) {}

    /**
     * {@code type}: REVIEW_REQUIRED, DIAGNOSING (the pending review is being diagnosed through its prerequisites),
     * CONSOLIDATE, MAINTAIN_DUE or NONE.
     */
    public record Action(String type, UUID reviewId, Instant dueAt) {}
}
