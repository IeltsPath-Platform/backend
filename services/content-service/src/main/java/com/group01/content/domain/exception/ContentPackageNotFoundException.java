package com.group01.content.domain.exception;

import java.util.UUID;

public class ContentPackageNotFoundException extends ContentDomainException {
    public ContentPackageNotFoundException(UUID id) {
        super("Content package not found with id: " + id);
    }

    public ContentPackageNotFoundException(String code) {
        super("Content package not found with code: " + code);
    }
}

