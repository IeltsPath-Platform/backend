package com.ieltspath.content.domain.vo;

/**
 * Whether an error type has been checked by a person who knows IELTS. A {@code DRAFT} type may be used for internal
 * error analysis, but it must be approved before learners see its code or fix hint.
 */
public enum ErrorReviewStatus {
    DRAFT,
    APPROVED
}
