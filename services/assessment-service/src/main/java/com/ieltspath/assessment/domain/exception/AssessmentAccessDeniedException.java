package com.ieltspath.assessment.domain.exception;

public class AssessmentAccessDeniedException extends AssessmentDomainException {
    public AssessmentAccessDeniedException() { super("Bạn không có quyền truy cập bài thi này."); }
    public AssessmentAccessDeniedException(String message) { super(message); }
}
