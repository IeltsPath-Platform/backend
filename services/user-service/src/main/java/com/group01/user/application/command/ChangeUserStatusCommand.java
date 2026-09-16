package com.group01.user.application.command;

import com.group01.user.domain.vo.UserStatus;

import java.util.UUID;

public record ChangeUserStatusCommand(UUID userId, UserStatus status) {
}
