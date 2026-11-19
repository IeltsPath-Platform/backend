package com.ieltspath.content.domain.exception;

public class DraftVersionExistsException extends ContentDomainException {
    public DraftVersionExistsException() {
        super("Gói này đã có một phiên bản nháp");
    }
}
