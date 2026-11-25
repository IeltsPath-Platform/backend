package com.ieltspath.user.application.exception;

public class ReauthRequiredException extends RuntimeException {
    public ReauthRequiredException() { super("Vui lòng xác thực lại để thực hiện thao tác này."); }
}
