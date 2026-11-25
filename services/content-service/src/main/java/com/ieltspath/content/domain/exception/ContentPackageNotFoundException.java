package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class ContentPackageNotFoundException extends ContentDomainException {
    public ContentPackageNotFoundException(UUID id) {
        super("Không tìm thấy gói nội dung");
    }

    public ContentPackageNotFoundException(String code) {
        super("Không tìm thấy gói nội dung");
    }
}

