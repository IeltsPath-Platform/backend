package com.ieltspath.assessment.domain.exception;

/** The chosen examiner cannot take another job: they stopped accepting submissions, or reached today's limit. */
public class ExaminerUnavailableException extends RuntimeException {
    public enum Reason { NOT_ACCEPTING, FULL }

    private final Reason reason;

    public ExaminerUnavailableException(Reason reason) {
        super(reason == Reason.FULL ? "The examiner has reached the limit of submissions for today"
                : "The examiner is not accepting submissions");
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
