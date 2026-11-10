package com.ieltspath.notification.application;

public interface PasswordResetMailer {
    void send(String email, String displayName, String resetLink);
}
