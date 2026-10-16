package com.ieltspath.assessment.application.exception;

import java.util.List;

/** An error analysis entry names a code that is not in the active error-type catalog. */
public class UnknownErrorCodeException extends RuntimeException {
    private final List<String> codes;

    public UnknownErrorCodeException(List<String> codes) {
        super("Unknown error code(s): " + String.join(", ", codes));
        this.codes = List.copyOf(codes);
    }

    public List<String> getCodes() {
        return codes;
    }
}
