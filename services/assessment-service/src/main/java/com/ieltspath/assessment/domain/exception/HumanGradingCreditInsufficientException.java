package com.ieltspath.assessment.domain.exception;

public class HumanGradingCreditInsufficientException extends AssessmentDomainException {
    public HumanGradingCreditInsufficientException() {
        super("No human grading credits are available");
    }
}
