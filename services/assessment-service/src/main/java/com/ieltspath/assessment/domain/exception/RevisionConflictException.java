package com.ieltspath.assessment.domain.exception;

public class RevisionConflictException extends AssessmentDomainException {
    public RevisionConflictException(long expected, long actual) {
        super("Dữ liệu bài làm đã thay đổi trên phiên khác. Vui lòng tải lại và thử lại.");
    }
    public RevisionConflictException(String message) { super(message); }
}
