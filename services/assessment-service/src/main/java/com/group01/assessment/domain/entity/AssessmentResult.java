package com.group01.assessment.domain.entity;

import com.group01.assessment.domain.exception.InvalidAssessmentStateException;

import java.time.Instant;
import java.util.UUID;

/**
 * One version of the formal result for an attempt. A version is editable while it is being graded and
 * immutable once COMPLETED; a regrade creates the next version for the same attempt.
 */
public record AssessmentResult(UUID id, UUID attemptId, int resultVersion, String status,
                               Double overallBand, Instant completedAt) {
    public static final String DRAFT = "DRAFT";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    public boolean isGradable() {
        return DRAFT.equals(status) || PROCESSING.equals(status);
    }

    /** The band a grader decided for this version; replaces any band set when the version was opened. */
    public AssessmentResult withGradedBand(Double band) {
        if (!isGradable()) {
            throw new InvalidAssessmentStateException("Only a draft or processing result can be graded");
        }
        return new AssessmentResult(id, attemptId, resultVersion, status, band, completedAt);
    }

    public AssessmentResult complete(Instant at) {
        if (!isGradable()) {
            throw new InvalidAssessmentStateException("Only a draft or processing result can be finalized");
        }
        return new AssessmentResult(id, attemptId, resultVersion, COMPLETED, overallBand, at);
    }
}
