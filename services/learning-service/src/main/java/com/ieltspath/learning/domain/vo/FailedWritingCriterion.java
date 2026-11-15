package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A mapped rubric criterion of an essay or a recorded answer whose band is below the pass band. {@code sourceSubmissionId}
 * is the essay whose grade found it weak; null when no essay did (a recorded answer, or a row stored before it was kept).
 * {@code reviewPoints} are the knowledge points of the errors this criterion's essay made most, best first; the review
 * takes the first one that can be practised and falls back to {@code knowledgePointId}. {@code reviewKnowledgePointId} is
 * the point a selected review will practise, null for the criterion's own point.
 */
public record FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band, UUID sourceSubmissionId,
                                     List<UUID> reviewPoints, UUID reviewKnowledgePointId) {
    public FailedWritingCriterion {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(band, "band");
        reviewPoints = reviewPoints == null ? List.of() : List.copyOf(reviewPoints);
    }

    public FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band, UUID sourceSubmissionId) {
        this(knowledgePointId, code, band, sourceSubmissionId, List.of(), null);
    }

    public FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band) {
        this(knowledgePointId, code, band, null);
    }

    /** The same criterion, remembering the essay whose grade found it weak. */
    public FailedWritingCriterion fromSubmission(UUID submissionId) {
        return new FailedWritingCriterion(knowledgePointId, code, band, submissionId, reviewPoints,
                reviewKnowledgePointId);
    }

    /** The same criterion with the knowledge points of its most frequent errors, best first. */
    public FailedWritingCriterion withReviewPoints(List<UUID> points) {
        return new FailedWritingCriterion(knowledgePointId, code, band, sourceSubmissionId, points,
                reviewKnowledgePointId);
    }

    /** The same criterion with the point its review will practise. */
    public FailedWritingCriterion reviewing(UUID point) {
        return new FailedWritingCriterion(knowledgePointId, code, band, sourceSubmissionId, reviewPoints, point);
    }

    /** The point a review of this criterion practises: the chosen error point, otherwise the criterion's own. */
    public UUID practisedPoint() {
        return reviewKnowledgePointId == null ? knowledgePointId : reviewKnowledgePointId;
    }
}
