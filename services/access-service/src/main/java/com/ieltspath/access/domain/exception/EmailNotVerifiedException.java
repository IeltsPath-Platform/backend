package com.ieltspath.access.domain.exception;

/** The learner must verify their email before redeeming an activation key. */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Vui lòng xác minh email trước khi nhập mã kích hoạt");
    }
}
