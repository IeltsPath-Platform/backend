package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class ErrorTypeNotFoundException extends ContentDomainException {
    public ErrorTypeNotFoundException(UUID id) {
        super("Error type not found: " + id);
    }
}
