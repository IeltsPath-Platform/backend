package com.ieltspath.user.application.command;

import com.ieltspath.user.domain.vo.UserStatus;

import java.util.UUID;

public record ChangeUserStatusCommand(UUID userId, UserStatus status) {
}
