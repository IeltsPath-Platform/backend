package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class SectionQuestionNotFoundException extends ContentDomainException {
    public SectionQuestionNotFoundException(UUID id) {
        super("Không tìm thấy câu hỏi trong phần của gói");
    }
}
