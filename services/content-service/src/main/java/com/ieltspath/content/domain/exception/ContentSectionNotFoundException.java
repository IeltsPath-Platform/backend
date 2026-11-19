package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class ContentSectionNotFoundException extends ContentDomainException {
    public ContentSectionNotFoundException(UUID id) {
        super("Không tìm thấy phần của gói");
    }
}
