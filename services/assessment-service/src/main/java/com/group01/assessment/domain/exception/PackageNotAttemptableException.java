package com.group01.assessment.domain.exception;

/** The Content package cannot be taken as an assessment attempt (for example a practice set or a lesson). */
public class PackageNotAttemptableException extends AssessmentDomainException {
    public PackageNotAttemptableException(String message) {
        super(message);
    }
}
