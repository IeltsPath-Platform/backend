package com.ieltspath.assessment.domain.exception;

public class AssessmentAccessDeniedException extends AssessmentDomainException {
    public AssessmentAccessDeniedException() { super("You do not have access to this assessment"); }
    public AssessmentAccessDeniedException(String message) { super(message); }
}
