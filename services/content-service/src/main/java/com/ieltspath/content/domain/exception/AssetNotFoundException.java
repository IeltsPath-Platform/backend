package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class AssetNotFoundException extends ContentDomainException {
    public AssetNotFoundException(UUID id) {
        super("Không tìm thấy tài nguyên nội dung");
    }
}

