package com.ieltspath.user.domain.exception;

import java.util.List;

public class WeakPasswordException extends RuntimeException {
    private final List<String> reasons;

    public WeakPasswordException(List<String> reasons) {
        super("Mật khẩu chưa đủ mạnh.");
        this.reasons = List.copyOf(reasons);
    }

    public List<String> reasons() {
        return reasons;
    }
}
