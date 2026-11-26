package com.ieltspath.assessment.domain.vo;

public enum HumanReviewStatus {
    QUEUED, ASSIGNED, IN_REVIEW, COMPLETED;

    public boolean canMoveTo(HumanReviewStatus next) {
        return switch (this) {
            case QUEUED -> next == ASSIGNED;
            case ASSIGNED -> next == QUEUED || next == IN_REVIEW;
            case IN_REVIEW -> next == QUEUED || next == COMPLETED;
            case COMPLETED -> next == QUEUED;
        };
    }
}
