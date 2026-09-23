package com.group01.content.domain.exception;

import java.util.UUID;

public class TopicNotFoundException extends ContentDomainException {
    public TopicNotFoundException(UUID id) {
        super("Topic not found with id: " + id);
    }

    public TopicNotFoundException(String code) {
        super("Topic not found with code: " + code);
    }
}

