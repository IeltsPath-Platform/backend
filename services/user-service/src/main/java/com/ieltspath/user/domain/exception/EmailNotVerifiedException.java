package com.ieltspath.user.domain.exception;

/** The account must prove its email address before this action is allowed. */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Vui lòng xác minh email trước khi thực hiện thao tác này");
    }
}
