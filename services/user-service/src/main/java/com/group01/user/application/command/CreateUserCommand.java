package com.group01.user.application.command;

import java.util.Set;

public record CreateUserCommand(String email, String password, String fullName, String phoneNumber, Set<String> roles) {
}
