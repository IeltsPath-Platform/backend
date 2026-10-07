package com.ieltspath.assessment.domain.exception;

public class RevisionConflictException extends AssessmentDomainException {
    public RevisionConflictException(long expected, long actual) {
        super("Response revision conflict. Expected " + expected + " but was " + actual);
    }
    public RevisionConflictException(String message) { super(message); }
}
