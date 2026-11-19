package com.ieltspath.content.domain.exception;

public class InvalidKnowledgePointCodeException extends ContentDomainException {
    public InvalidKnowledgePointCodeException(String code) {
        super("Mã điểm kiến thức \"" + code + "\" không đúng định dạng");
    }
}
