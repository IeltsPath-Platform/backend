package com.ieltspath.content.domain.exception;

/** An edited draft item breaks a rule a question must follow; {@code reason} is the same code a rejected item carries. */
public class InvalidDraftItemException extends ContentDomainException {

    private final String reason;

    public InvalidDraftItemException(String reason) {
        super("The edited question is not usable: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
