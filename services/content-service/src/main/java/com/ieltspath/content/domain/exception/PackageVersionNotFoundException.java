package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class PackageVersionNotFoundException extends ContentDomainException {
    public PackageVersionNotFoundException(UUID id) {
        super("Không tìm thấy phiên bản đã xuất bản của gói");
    }
}
