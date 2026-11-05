package com.ieltspath.assessment.domain.exception;

/** A learner may cancel only a late job whose examiner can no longer be changed. */
public class GradingCancelNotAllowedException extends RuntimeException {
    public enum Reason { NOT_OVERDUE, CHANGES_LEFT }

    private final Reason reason;

    public GradingCancelNotAllowedException(Reason reason) {
        super(switch (reason) {
            case NOT_OVERDUE -> "The examiner can still be waited for: the deadline has not passed";
            case CHANGES_LEFT -> "The examiner can still be changed; cancel once no change is left";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
