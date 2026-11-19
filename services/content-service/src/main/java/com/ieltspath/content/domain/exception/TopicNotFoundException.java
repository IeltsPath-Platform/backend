package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class TopicNotFoundException extends ContentDomainException {
    public TopicNotFoundException(UUID id) {
        super("Không tìm thấy chủ đề");
    }

    public TopicNotFoundException(String code) {
        super("Không tìm thấy chủ đề");
    }
}

