package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The learner report: for each knowledge point the learner has evidence on, a fixed Score / Errors / Evidence / Action
 * block. {@code code}, {@code name}, {@code skill} and {@code band} come from Content and are null when it does not
 * list the point. {@code skills} groups mastery per skill and per foundation kind, {@code focus} names the few points to
 * work on first, {@code estimatedBand} is an estimate and not an exam result, and {@code target} is the learner's goal
 * when the User Service could give it.
 */
public record LearnerReportResult(UUID userId, Instant generatedAt, List<KnowledgePointReport> knowledgePoints,
                                  List<SkillSummary> skills, List<FocusPoint> focus, EstimatedBand estimatedBand,
                                  Target target) {
    public LearnerReportResult(UUID userId, Instant generatedAt, List<KnowledgePointReport> knowledgePoints) {
        this(userId, generatedAt, knowledgePoints, List.of(), List.of(), null, null);
    }

    /**
     * {@code mastery} is the mean over judged points weighted by evidence, or null when none is judged. {@code band} is
     * the estimated band of the skill and is null for a foundation kind.
     */
    public record SkillSummary(String skill, Double mastery, Map<String, Integer> tierCounts, int kpCount,
                               BigDecimal band) {}

    public record FocusPoint(UUID knowledgeId, String code, String name, String skill, double mastery, String tier,
                             boolean reviewRequired) {}

    /** {@code value} is null when no skill has a band; {@code estimated} is always true. */
    public record EstimatedBand(BigDecimal value, boolean estimated, boolean speakingIncluded,
                                List<NextBandHint> nextBandHints) {}

    public record NextBandHint(String skill, BigDecimal nextBand, List<UUID> knowledgeIds) {}

    /**
     * {@code gap} is the target minus the estimated band, null when either is unknown. {@code projection} is the date the
     * target is reached at the recent pace, or the reason there is none; null without a target band.
     */
    public record Target(BigDecimal targetBand, BigDecimal gap, LocalDate examDate,
                         SnapshotReportResult.Projection projection) {}

    public record KnowledgePointReport(UUID knowledgeId, String code, String name, String skill, BigDecimal band,
                                       Score score, List<ErrorEntry> errors, List<EvidenceEntry> evidence,
                                       Action action) {}

    public record Score(double mastery, String tier, int evidenceCount) {}

    public record ErrorEntry(String code, int count) {}

    /**
     * {@code revisionNo} and {@code revisionDelta} are present only on an essay: its version number and its overall band
     * minus the band of the version it revised (absent for a first essay). The grading is automatic, so a delta also
     * carries the grader's noise.
     */
    public record EvidenceEntry(UUID questionVersionId, Boolean correct, Long durationMs, String source,
                                String errorCode, Instant at,
                                @com.fasterxml.jackson.annotation.JsonInclude(
                                        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                                Integer revisionNo,
                                @com.fasterxml.jackson.annotation.JsonInclude(
                                        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                                BigDecimal revisionDelta) {
        public EvidenceEntry(UUID questionVersionId, Boolean correct, Long durationMs, String source,
                             String errorCode, Instant at) {
            this(questionVersionId, correct, durationMs, source, errorCode, at, null, null);
        }
    }

    /**
     * {@code type}: REVIEW_REQUIRED, DIAGNOSING (the pending review is being diagnosed through its prerequisites),
     * CONSOLIDATE, MAINTAIN_DUE or NONE.
     */
    public record Action(String type, UUID reviewId, Instant dueAt) {}
}
