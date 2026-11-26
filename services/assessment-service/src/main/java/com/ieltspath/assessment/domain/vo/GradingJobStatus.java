package com.ieltspath.assessment.domain.vo;

public enum GradingJobStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED;

    public boolean canMoveTo(GradingJobStatus next) {
        return switch (this) {
            case QUEUED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == QUEUED || next == COMPLETED || next == FAILED || next == CANCELLED;
            // Explicit regrade creates a new assessment result version and requeues this grading job.
            case COMPLETED -> next == QUEUED;
            case FAILED, CANCELLED -> false;
        };
    }
}
