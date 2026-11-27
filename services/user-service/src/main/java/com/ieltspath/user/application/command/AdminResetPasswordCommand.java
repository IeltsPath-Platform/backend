package com.ieltspath.user.application.command;

import java.util.UUID;

public record AdminResetPasswordCommand(UUID adminId, UUID userId, String newPassword) { }
