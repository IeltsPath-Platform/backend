package com.ieltspath.content.domain.exception;

public class AiQuotaExceededException extends ContentDomainException {
    public AiQuotaExceededException() {
        super("Daily AI allowance is used up");
    }
}
