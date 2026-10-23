package com.ieltspath.content.domain.exception;

/** The author has used today's import allowance. Nothing was written. */
public class ImportQuotaExceededException extends ContentDomainException {
    public ImportQuotaExceededException() {
        super("The daily import allowance is used up");
    }
}
