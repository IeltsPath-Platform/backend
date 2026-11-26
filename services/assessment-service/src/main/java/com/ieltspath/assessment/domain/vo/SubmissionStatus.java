package com.ieltspath.assessment.domain.vo;

public enum SubmissionStatus {
    DRAFT, SUBMITTED;

    public boolean canMoveTo(SubmissionStatus next) {
        return this == DRAFT && next == SUBMITTED;
    }
}
