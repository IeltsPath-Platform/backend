package com.ieltspath.assessment.domain.exception;

public class HumanGradingCreditConflictException extends AssessmentDomainException {
    public HumanGradingCreditConflictException() {
        super("The human grading request conflicts with a previous request");
    }
}
