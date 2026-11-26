package com.ieltspath.assessment.domain.exception;

public class HumanGradingCreditConflictException extends AssessmentDomainException {
    public HumanGradingCreditConflictException() {
        super("Yêu cầu chấm bài bởi giám khảo bị xung đột với một yêu cầu trước đó.");
    }
}
