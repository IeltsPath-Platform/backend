package com.group01.user.application.command;

import java.util.Set;
import java.util.UUID;

public record AssignRoleCommand(UUID userId, Set<String> roles) {
}
