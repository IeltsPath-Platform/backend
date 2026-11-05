package com.ieltspath.assessment.domain.exception;

/** A learner may replace the examiner only after the deadline passed, and only a limited number of times. */
public class ExaminerChangeNotAllowedException extends RuntimeException {
    public enum Reason { NOT_OVERDUE, LIMIT_REACHED, SAME_EXAMINER }

    private final Reason reason;

    public ExaminerChangeNotAllowedException(Reason reason) {
        super(switch (reason) {
            case NOT_OVERDUE -> "The examiner can still be waited for: the deadline has not passed";
            case LIMIT_REACHED -> "The examiner of this submission was already changed the most times allowed";
            case SAME_EXAMINER -> "Choose an examiner other than the current one";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
