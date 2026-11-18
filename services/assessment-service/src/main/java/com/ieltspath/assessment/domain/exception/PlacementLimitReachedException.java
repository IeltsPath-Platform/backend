package com.ieltspath.assessment.domain.exception;

public class PlacementLimitReachedException extends AssessmentDomainException {
    public PlacementLimitReachedException() { super("Placement attempts are limited to three per rolling 24 hours"); }
}
