package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class KnowledgePointInUseException extends ContentDomainException {
    public KnowledgePointInUseException(UUID id) {
        super("Knowledge point is already used by a published question or lesson: " + id);
    }
}
