package com.ieltspath.user.application.command;

public record ResetPasswordCommand(
        String token,
        String newPassword
) {
}

