package com.ieltspath.user.api.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record ReauthRequest(String currentPassword, String googleIdToken) {
    @AssertTrue(message = "Hãy cung cấp đúng một cách xác thực lại.")
    public boolean hasExactlyOneCredential() {
        boolean hasPassword = currentPassword != null && !currentPassword.isBlank();
        boolean hasGoogleToken = googleIdToken != null && !googleIdToken.isBlank();
        return hasPassword ^ hasGoogleToken;
    }
}
