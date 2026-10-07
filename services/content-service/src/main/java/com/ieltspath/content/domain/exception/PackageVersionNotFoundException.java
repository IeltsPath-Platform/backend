package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class PackageVersionNotFoundException extends ContentDomainException {
    public PackageVersionNotFoundException(UUID id) {
        super("Published package version not found with id: " + id);
    }
}
