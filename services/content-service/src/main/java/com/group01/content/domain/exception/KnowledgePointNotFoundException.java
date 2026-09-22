package com.group01.content.domain.exception;

import java.util.UUID;

public class KnowledgePointNotFoundException extends ContentDomainException {
    public KnowledgePointNotFoundException(UUID id) {
        super("Knowledge point not found with id: " + id);
    }

    public KnowledgePointNotFoundException(String code) {
        super("Knowledge point not found with code: " + code);
    }
}

