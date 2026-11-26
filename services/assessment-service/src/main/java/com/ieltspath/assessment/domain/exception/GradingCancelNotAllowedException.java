package com.ieltspath.assessment.domain.exception;

/** A learner may cancel only a late job whose examiner can no longer be changed. */
public class GradingCancelNotAllowedException extends RuntimeException {
    public enum Reason { NOT_OVERDUE, CHANGES_LEFT }

    private final Reason reason;

    public GradingCancelNotAllowedException(Reason reason) {
        super(switch (reason) {
            case NOT_OVERDUE -> "Chưa quá hạn chấm bài nên chưa thể hủy yêu cầu chấm.";
            case CHANGES_LEFT -> "Bạn vẫn còn lượt đổi giám khảo; chỉ có thể hủy sau khi đã hết lượt đổi.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
