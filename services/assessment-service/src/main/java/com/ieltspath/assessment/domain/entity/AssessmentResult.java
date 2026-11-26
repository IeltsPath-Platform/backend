package com.ieltspath.assessment.domain.entity;

import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;

import java.time.Instant;
import java.util.UUID;
import com.ieltspath.assessment.domain.vo.PlacementPool;

/**
 * One version of the formal result for an attempt. A version is editable while it is being graded and
 * immutable once COMPLETED; a regrade creates the next version for the same attempt.
 */
public record AssessmentResult(UUID id, UUID attemptId, int resultVersion, String status,
                               Double overallBand, Instant completedAt, PlacementPool placementPool,
                               Double uncappedBand, boolean partial) {
    public static final String DRAFT = "DRAFT";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public AssessmentResult(UUID id, UUID attemptId, int resultVersion, String status,
                            Double overallBand, Instant completedAt) {
        this(id, attemptId, resultVersion, status, overallBand, completedAt, null, null, false);
    }

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    public boolean isGradable() {
        return DRAFT.equals(status) || PROCESSING.equals(status);
    }

    /** The band a grader decided for this version; replaces any band set when the version was opened. */
    public AssessmentResult withGradedBand(Double band) {
        if (!isGradable()) {
            throw new InvalidAssessmentStateException("Chỉ có thể chấm kết quả đang ở trạng thái nháp hoặc đang xử lý.");
        }
        return new AssessmentResult(id, attemptId, resultVersion, status, band, completedAt,
                placementPool, uncappedBand, partial);
    }

    public AssessmentResult complete(Instant at) {
        if (!isGradable()) {
            throw new InvalidAssessmentStateException("Chỉ có thể hoàn tất kết quả đang ở trạng thái nháp hoặc đang xử lý.");
        }
        return new AssessmentResult(id, attemptId, resultVersion, COMPLETED, overallBand, at,
                placementPool, uncappedBand, partial);
    }
}
