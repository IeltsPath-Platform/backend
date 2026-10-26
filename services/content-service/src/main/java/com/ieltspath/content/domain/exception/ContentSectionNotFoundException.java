package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class ContentSectionNotFoundException extends ContentDomainException {
    public ContentSectionNotFoundException(UUID id) {
        super("Section not found: " + id);
    }
}
