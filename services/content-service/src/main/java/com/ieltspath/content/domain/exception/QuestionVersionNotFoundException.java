package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class QuestionVersionNotFoundException extends ContentDomainException {
    public QuestionVersionNotFoundException(UUID id) {
        super("Không tìm thấy phiên bản câu hỏi");
    }
}
