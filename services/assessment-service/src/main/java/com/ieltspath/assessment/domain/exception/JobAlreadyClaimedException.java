package com.ieltspath.assessment.domain.exception;

public class JobAlreadyClaimedException extends RuntimeException {
    public JobAlreadyClaimedException() { super("Grading job is already claimed or no longer available"); }
}
