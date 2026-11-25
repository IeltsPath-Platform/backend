package com.ieltspath.content.domain.exception;

public class PackageArchiveNotSupportedException extends ContentDomainException {
    public PackageArchiveNotSupportedException() {
        super("Chỉ đề luyện tập và đề đầu vào mới ẩn hoặc gỡ được.");
    }
}
