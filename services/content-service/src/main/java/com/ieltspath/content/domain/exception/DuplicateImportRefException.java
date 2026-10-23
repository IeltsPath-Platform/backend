package com.ieltspath.content.domain.exception;

public class DuplicateImportRefException extends ContentDomainException {
    public DuplicateImportRefException(String importRef) {
        super("importRef is already used by another question: " + importRef);
    }
}
