package com.ieltspath.content.domain.exception;

/** The author has used today's import allowance. Nothing was written. */
public class ImportQuotaExceededException extends ContentDomainException {
    public ImportQuotaExceededException() {
        super("Bạn đã dùng hết lượt nhập tệp trong ngày");
    }
}
