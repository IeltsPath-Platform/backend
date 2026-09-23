package com.group01.content.domain.exception;

public class DuplicateCodeException extends ContentDomainException {
    public DuplicateCodeException(String entityName, String code) {
        super(entityName + " with code '" + code + "' already exists");
    }
}

