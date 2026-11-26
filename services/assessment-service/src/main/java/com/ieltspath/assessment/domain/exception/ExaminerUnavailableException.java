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
            case FULL -> "Giám khảo đã nhận đủ số bài chấm trong hôm nay. Vui lòng chọn giám khảo khác.";
            case NOT_ACCEPTING -> "Giám khảo hiện không nhận chấm bài mới. Vui lòng chọn giám khảo khác.";
            case OVERDUE_BACKLOG -> "Giám khảo đang có quá nhiều bài quá hạn nên tạm thời không nhận thêm bài mới.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
