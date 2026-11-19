package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class KnowledgePointNotFoundException extends ContentDomainException {
    public KnowledgePointNotFoundException(UUID id) {
        super("Không tìm thấy điểm kiến thức");
    }

    public KnowledgePointNotFoundException(String code) {
        super("Không tìm thấy điểm kiến thức");
    }
}

