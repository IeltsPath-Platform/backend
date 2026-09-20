package com.group01.user.application.command;

public record ResetPasswordCommand(
        String token,
        String newPassword
) {
}

