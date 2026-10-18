package com.ieltspath.content.domain.exception;

import java.util.List;

/** A distractor names an error code that is not an active error type. */
public class UnknownErrorCodeException extends ContentDomainException {
    private final List<String> codes;

    public UnknownErrorCodeException(List<String> codes) {
        super("Unknown or inactive error code(s): " + String.join(", ", codes));
        this.codes = List.copyOf(codes);
    }

    public List<String> getCodes() {
        return codes;
    }
}
