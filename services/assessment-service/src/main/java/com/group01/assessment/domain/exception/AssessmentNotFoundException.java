package com.group01.assessment.domain.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends AssessmentDomainException {
    public AssessmentNotFoundException(UUID id) { super("Assessment attempt not found: " + id); }
    public AssessmentNotFoundException(String message) { super(message); }
}
