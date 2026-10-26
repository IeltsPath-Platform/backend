package com.ieltspath.content.domain.exception;

/** The draft has no item at this position. */
public class DraftItemNotFoundException extends ContentDomainException {

    public DraftItemNotFoundException(int index) {
        super("The draft has no item " + index);
    }
}
