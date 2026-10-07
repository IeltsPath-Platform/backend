package com.ieltspath.assessment.domain.exception;

/** A submit arrived at or after the attempt's deadline; the attempt is (and stays) EXPIRED. */
public class AttemptExpiredException extends AssessmentDomainException {
    public AttemptExpiredException() {
        super("Attempt has expired");
    }
}
