package com.ieltspath.content.domain.exception;

public class VersionNotDraftException extends InvalidContentStateException {
    public VersionNotDraftException() {
        super("Chỉ sửa được phiên bản nháp");
    }
}
