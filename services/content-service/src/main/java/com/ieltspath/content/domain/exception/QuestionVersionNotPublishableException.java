package com.ieltspath.content.domain.exception;

public class QuestionVersionNotPublishableException extends ContentDomainException {
    public QuestionVersionNotPublishableException() {
        super("Phiên bản câu hỏi chưa đủ điều kiện để xuất bản");
    }
}
