package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.PasswordBlocklist;
import com.ieltspath.user.application.port.PasswordPolicySettings;
import com.ieltspath.user.domain.exception.WeakPasswordException;
import com.ieltspath.user.domain.vo.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {
    private final PasswordBlocklist blocklist;
    private final PasswordPolicySettings properties;

    public void check(String password, String email, String fullName, String phone) {
        if (password == null || password.length() < properties.minLength()) {
            throw new WeakPasswordException(java.util.List.of("TOO_SHORT"));
        }
        if (PasswordPolicy.exceedsMaxBytes(password)) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
        }
        var reasons = new ArrayList<String>();
        if (blocklist.contains(password)) reasons.add("TOO_COMMON");
        String normalized = password.toLowerCase(Locale.ROOT).trim();
        int separator = email == null ? -1 : email.indexOf('@');
        String localPart = separator > 0 ? email.substring(0, separator) : null;
        if (matches(normalized, localPart) || matches(normalized, fullName) || matches(normalized, phone)) {
            reasons.add("CONTAINS_ACCOUNT_INFO");
        }
        if (PasswordPolicy.isTooSimple(password)) reasons.add("TOO_SIMPLE");
        if (!reasons.isEmpty()) throw new WeakPasswordException(reasons);
    }

    private boolean matches(String password, String value) {
        return value != null && !value.isBlank() && password.equals(value.toLowerCase(Locale.ROOT).trim());
    }
}
