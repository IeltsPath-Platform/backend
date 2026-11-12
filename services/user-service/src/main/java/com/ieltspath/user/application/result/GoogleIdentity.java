package com.ieltspath.user.application.result;

import java.util.Locale;

public record GoogleIdentity(String subject, String email, String name, String hostedDomain) {
    public boolean isEmailAuthoritative() {
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            return true;
        }
        return hostedDomain != null && !hostedDomain.isBlank()
                && domain.equals(hostedDomain.trim().toLowerCase(Locale.ROOT));
    }
}
