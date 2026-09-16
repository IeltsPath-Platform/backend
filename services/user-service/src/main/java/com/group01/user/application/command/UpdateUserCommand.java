package com.group01.user.application.command;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String fullName, String phoneNumber) {
}
