package com.ieltspath.assessment.domain.exception;

/** A learner may replace the examiner only after the deadline passed, and only a limited number of times. */
public class ExaminerChangeNotAllowedException extends RuntimeException {
    public enum Reason { NOT_OVERDUE, LIMIT_REACHED, SAME_EXAMINER }

    private final Reason reason;

    public ExaminerChangeNotAllowedException(Reason reason) {
        super(switch (reason) {
            case NOT_OVERDUE -> "Chưa quá hạn chấm bài nên chưa thể đổi giám khảo.";
            case LIMIT_REACHED -> "Bạn đã đổi giám khảo cho bài nộp này đạt số lần tối đa cho phép.";
            case SAME_EXAMINER -> "Vui lòng chọn giám khảo khác với giám khảo hiện tại.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
