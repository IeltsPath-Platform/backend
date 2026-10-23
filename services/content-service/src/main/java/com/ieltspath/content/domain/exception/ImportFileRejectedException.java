package com.ieltspath.content.domain.exception;

/** The uploaded file cannot be an import workbook at all: wrong type or too large. */
public class ImportFileRejectedException extends ContentDomainException {

    public static final String NOT_XLSX = "NOT_XLSX";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";

    private final String code;

    public ImportFileRejectedException(String code) {
        super(NOT_XLSX.equals(code) ? "The file is not an .xlsx workbook" : "The file is larger than the limit");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
