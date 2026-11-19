package com.ieltspath.content.domain.exception;

public class AiQuotaExceededException extends ContentDomainException {
    public AiQuotaExceededException() {
        super("Bạn đã dùng hết lượt AI trong ngày");
    }
}
