package com.ieltspath.user.config;

import org.springframework.stereotype.Component;

@Component
public class AuthCookieSecurityGuard {
    public AuthCookieSecurityGuard(AuthCookieProperties properties) {
        if (properties.enforceSecure() && !properties.secure()) {
            throw new IllegalStateException("Cookie xác thực phải bật Secure khi enforce-secure được bật.");
        }
    }
}
