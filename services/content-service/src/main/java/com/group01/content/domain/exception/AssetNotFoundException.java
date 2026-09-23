package com.group01.content.domain.exception;

import java.util.UUID;

public class AssetNotFoundException extends ContentDomainException {
    public AssetNotFoundException(UUID id) {
        super("Content asset not found with id: " + id);
    }
}

