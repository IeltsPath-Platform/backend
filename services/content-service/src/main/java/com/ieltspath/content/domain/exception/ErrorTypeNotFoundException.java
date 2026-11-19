package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class ErrorTypeNotFoundException extends ContentDomainException {
    public ErrorTypeNotFoundException(UUID id) {
        super("Không tìm thấy loại lỗi");
    }
}
