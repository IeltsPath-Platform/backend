package com.ieltspath.content.domain.exception;

public class InvalidKnowledgePointCodeException extends ContentDomainException {
    public InvalidKnowledgePointCodeException(String code) {
        super("Knowledge point code has an invalid format: " + code);
    }
}
