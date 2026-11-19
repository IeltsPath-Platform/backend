package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class QuestionVersionInUseException extends ContentDomainException {
    public QuestionVersionInUseException(UUID id) {
        super("Phiên bản câu hỏi đang được nội dung đã xuất bản sử dụng");
    }
}
