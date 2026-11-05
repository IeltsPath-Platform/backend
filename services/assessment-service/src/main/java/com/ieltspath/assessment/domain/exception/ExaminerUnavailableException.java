package com.ieltspath.assessment.domain.exception;

/**
 * The chosen examiner cannot take another job: they stopped accepting submissions, reached today's limit, or have too many
 * late jobs to grade first.
 */
public class ExaminerUnavailableException extends RuntimeException {
    public enum Reason { NOT_ACCEPTING, FULL, OVERDUE_BACKLOG }

    private final Reason reason;

    public ExaminerUnavailableException(Reason reason) {
        super(switch (reason) {
            case FULL -> "The examiner has reached the limit of submissions for today";
            case NOT_ACCEPTING -> "The examiner is not accepting submissions";
            case OVERDUE_BACKLOG -> "The examiner has too many late submissions and takes no new ones for now";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
