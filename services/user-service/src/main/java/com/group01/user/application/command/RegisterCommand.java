package com.group01.user.application.command;

public record RegisterCommand(
        String email,
        String password,
        String fullName,
        String phoneNumber,
        String role
) {
}
