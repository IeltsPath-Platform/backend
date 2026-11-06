package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A mapped rubric criterion of an essay or a recorded answer whose band is below the pass band. {@code sourceSubmissionId}
 * is the essay whose grade found it weak; null when no essay did (a recorded answer, or a row stored before it was kept).
 */
public record FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band, UUID sourceSubmissionId) {
    public FailedWritingCriterion {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(band, "band");
    }

    public FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band) {
        this(knowledgePointId, code, band, null);
    }

    /** The same criterion, remembering the essay whose grade found it weak. */
    public FailedWritingCriterion fromSubmission(UUID submissionId) {
        return new FailedWritingCriterion(knowledgePointId, code, band, submissionId);
    }
}
