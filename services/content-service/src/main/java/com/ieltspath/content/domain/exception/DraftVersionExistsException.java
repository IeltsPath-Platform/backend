package com.ieltspath.content.domain.exception;

public class DraftVersionExistsException extends ContentDomainException {
    public DraftVersionExistsException() {
        super("The package already has a draft version");
    }
}
