package com.ieltspath.content.domain.exception;

public class DuplicateImportRefException extends ContentDomainException {
    public DuplicateImportRefException(String importRef) {
        super("Mã tham chiếu \"" + importRef + "\" đã được dùng cho một câu hỏi khác");
    }
}
